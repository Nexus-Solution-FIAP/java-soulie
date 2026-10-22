# Fluxo de validação de imagem por IA

Como uma foto vira XP, e o que acontece quando algo dá errado no meio.

## Por que isto existe

Os dois repositórios que formaram este projeto esperavam esta peça, e nenhum a
implementou:

- No `soulie-soul-up`, os comentários do código diziam *"a evidência fica PENDING
  até a IA externa validar"* e *"quem aprova/reprova é a IA externa"*. Como nada
  nunca gravava `APROVED`, o `ApprovedEvidenceValidator` bloqueava e
  **`PUT /mission/{id}/complete` era impossível de concluir**.
- No `java-soulie`, `POST /v1/missions/{id}/validate` já recebia a foto e já
  devolvia um campo `aiDetections` — mas **descartava os bytes** e creditava o XP
  na hora, inclusive quando nenhuma foto era enviada. O `aiDetections` vinha de
  uma tabela de frases fixas escolhidas por palavra-chave na tag da missão
  ("Lixeiras de coleta seletiva identificadas"), apresentada ao usuário como
  análise de imagem.

Um pipeline só resolve os dois.

## O caminho feliz

```
  app
   │  POST /v1/missions/{id}/validate   (multipart: photo)
   ▼
┌──────────────────────── MissionApiService.validate ────────────────────────┐
│ 1. a missão existe? já foi feita no período?                               │
│ 2. a fila está acessível?            ──não──►  503, nada é gravado         │
│ 3. EvidenceService.submitForDailyMission:                                  │
│      · magic bytes  ──não é imagem──►  400                                 │
│      · sobe no bucket com chave UUID gerada pelo servidor                  │
│      · grava Evidence + Validation(PENDING)                                │
│      · dispara ValidationRequestedEvent                                    │
└────────────────────────────────────────────────────────────────────────────┘
   │
   ├──► 202 Accepted { validationId, status: "PENDING", xpGained: 0 }
   │
   │  (transação confirma)
   ▼
ValidationPublisher  @TransactionalEventListener(AFTER_COMMIT)
   │  publica { validationId } na exchange
   ▼
RabbitMQ  soulie.validation → fila soulie.validation.requested
   ▼
ValidationConsumer  @RabbitListener
   │  já resolvida? ──sim──► ignora (reentrega é normal)
   │  registra tentativa
   │  ImageValidationClient.validate(URL assinada + contexto da missão)
   ▼
API da IA do parceiro  ──► { status, detections, confidence, reason }
   ▼
ValidationOutcomeService.aplicar
   │  ValidationService.resolve  ──já resolvida──► para aqui, nada acontece
   │
   ├─ APPROVED → MissionApiService.rewardAprovada
   │               XP + pontos + impacto + streak + UserActivity + conquistas
   │             + notificação + push Expo
   ├─ REJECTED → notificação com o motivo que a IA deu
   └─ FAILED   → notificação dizendo que a falha foi nossa; reprocessável
```

## As peças e o que cada uma faz

| Classe | Papel |
|---|---|
| `MissionApiController` | Recebe o multipart, responde **202**. Também expõe `GET /v1/missions/validations/{id}`. |
| `MissionApiService.validate` | Orquestra: checa período, exige fila, delega o upload, devolve o `validationId`. |
| `EvidenceService.submitForDailyMission` | Sobe a imagem **antes** de gravar no banco, e cria `Evidence` + `Validation`. |
| `ImageType` | Decide o formato pelos **magic bytes**. |
| `S3CompatibleImageStorage` | Guarda no bucket e gera a URL assinada. |
| `ValidationService.createPending` | Cria a validação e dispara o evento interno. |
| `ValidationPublisher` | Publica na fila, **depois do commit**. |
| `ValidationConsumer` | Consome, chama a IA, repassa o veredito. |
| `ImageValidationClient` | A chamada HTTP de saída, no molde do `ExpoPushService`. |
| `ValidationService.resolve` | A transição PENDING → final, **uma vez só**. |
| `ValidationOutcomeService` | As consequências: recompensa, notificação, push. |
| `ValidationWebhookController` | Caminho alternativo, se a IA responder depois. |

## Por que cada decisão não óbvia está assim

### A publicação acontece depois do commit

`ValidationPublisher` escuta em `AFTER_COMMIT`, não publica direto no serviço.
Publicar dentro da transação é uma corrida clássica: o worker é rápido, consome
a mensagem antes do commit, não encontra a validação no banco, e a evidência
fica `PENDING` para sempre. O bug só aparece sob carga — que é exatamente quando
ninguém quer descobri-lo.

### A resolução é um UPDATE condicional, não um `if` em Java

```java
update Validation v set v.resultValidation = :resultado
 where v.id = :validationId and v.resultValidation = PENDING
```

Reentrega de mensagem é **comportamento normal** do RabbitMQ: acontece em
reconexão, em ack perdido, em redeploy no meio do processamento. Se o worker
lesse, checasse em memória e gravasse, duas entregas simultâneas passariam as
duas pela checagem e **creditariam o XP duas vezes**. É o mesmo problema do
duplo gasto no resgate de recompensa, e a solução é a mesma: o banco decide,
numa instrução só, e quem chama confere o rowcount.

### `FAILED` não é `REJECTED`

Qualquer resposta que não seja `APPROVED` ou `REJECTED` vira `FAILED`.

- **REJECTED** = "a foto não comprova a ação" → o usuário é avisado e pode tentar de novo.
- **FAILED** = "não conseguimos analisar" → o usuário é avisado de que o problema foi **nosso**.

Tratar os dois igual puniria quem fez a ação corretamente por uma falha de
infraestrutura. Era por isso que o enum ganhou `FAILED` na migration V20.

### A chave do objeto é UUID do servidor

A versão anterior guardava `photo.getOriginalFilename()` — um valor que vem do
cliente. Usar isso como caminho abre path traversal e permite que um usuário
sobrescreva o arquivo de outro.

### O tipo vem dos bytes, não do cabeçalho

`Content-Type` é informado pelo cliente e não custa nada falsificar: dá para
mandar um executável ou um HTML com script declarando `image/jpeg`. SVG fica
deliberadamente de fora da lista de aceitos — é XML e pode conter script.

### O bucket é privado e a IA lê por URL assinada

Foto de evidência costuma mostrar a casa e a rotina da pessoa. A URL assinada
deixa a IA ler sem receber credencial nenhuma, e expira sozinha em 15 minutos.

### A URL da IA vem só de configuração

Se o destino da chamada pudesse ser influenciado por quem envia a evidência, o
worker viraria um proxy de SSRF contra a rede interna. A validação de
`Evidence.image` (Fase 3b) fechou a porta de entrada; esta regra fecha a de
saída.

## O que acontece quando dá errado

| Situação | Comportamento |
|---|---|
| Arquivo não é imagem | 400 na hora, nada é gravado nem enviado ao bucket |
| Arquivo acima de 10MB | 400 na hora |
| Bucket fora do ar | 400 com mensagem genérica; o detalhe fica no log |
| RabbitMQ fora **antes** de gravar | 503, nada é gravado |
| RabbitMQ fora **na publicação** | A evidência está salva e `PENDING`. Nada se perde, mas precisa ser reenfileirada. Ver `docs/08-operacao.md` |
| IA não responde | Retry com espera crescente (4 tentativas). Esgotado → DLQ → `FAILED` |
| IA responde algo inesperado | `FAILED`, não `REJECTED` |
| Mensagem reentregue | Ignorada: a validação já não está `PENDING` |
| Webhook com assinatura errada | 401 |
| Webhook repetido | Idempotente: não credita duas vezes |

A **DLQ não tem TTL**, de propósito: o que cai lá é trabalho de uma pessoa real
que fez a ação e não conseguiu ser analisada. Expirar essas mensagens seria
descartar isso em silêncio.

## O que mudou para o app

Nenhum campo de `MissionValidationResponse` foi removido ou renomeado. Entraram
dois: `validationId` e `status`. O que mudou é **quando** os valores ficam
preenchidos.

| Antes | Agora |
|---|---|
| 200 com `xpGained` preenchido na hora | **202** com `status: "PENDING"` e `xpGained: 0` |
| Foto opcional | Foto **obrigatória** |
| `aiDetections` com frases fixas | `aiDetections` vazio até a IA responder |
| — | `GET /v1/missions/validations/{id}` para consultar |
| — | Push quando o resultado sai |

Um cliente antigo continua desserializando normalmente e lê `xpGained: 0`. A
configuração `soulie.ai.mode: sync` existe para o app poder migrar sem quebrar
de uma vez.

## Configuração

Tudo em `.env` (veja `.env.example`): `S3_*` para o bucket, `RABBITMQ_*` para a
fila, `AI_*` para a IA. O contrato que o parceiro precisa implementar está em
[`04-contrato-ia.md`](04-contrato-ia.md).
