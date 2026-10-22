# Contrato da API de validação de imagem

> **Este é o documento para entregar a quem vai construir a IA.** Ele descreve o
> que o Soulie envia, o que espera de volta, e como assinar a resposta se a
> análise não for imediata.

## O problema a resolver

O usuário tira uma foto para comprovar que fez uma missão ecológica — separar
recicláveis, ir de bicicleta, tomar banho mais curto. A pergunta que a IA
precisa responder **não é** "o que há nesta foto?", e sim:

> **Esta foto comprova _esta_ missão específica?**

Por isso o contexto da missão vai junto da imagem. Sem ele não há como
responder o que interessa.

## Fluxo

```
app ──POST /v1/missions/{id}/validate (multipart, foto)
      │ o Soulie valida a imagem por magic bytes, guarda no bucket
      │ e cria a validação como PENDING
      └──► 202 Accepted { validationId, status: "PENDING" }

Soulie (worker da fila) ──POST na SUA API──► você analisa
                        ◄── resposta com o veredito

      │ aprovado  → XP creditado + push para o usuário
      │ reprovado → notificação com o motivo
      └ falhou    → reprocessável, o usuário não perde o que fez
```

O Soulie chama você **de dentro de um worker de fila**, não durante a requisição
do usuário. Isso significa que **você pode demorar**: o limite é o timeout de
leitura, configurável e por padrão 30 segundos. Se precisar de mais, use o
webhook (seção final).

---

## 1. Requisição — o que você recebe

`POST <a URL que você nos informar>`

Cabeçalhos:

| Cabeçalho | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `Authorization` | `Bearer <a chave que você nos informar>` |

Corpo:

```json
{
  "validationId": 42,
  "imageUrl": "https://t3.storageapi.dev/images-.../evidencias/2026-10-21/8f14e45f-....jpg?X-Amz-Signature=...",
  "imageBase64": null,
  "contentType": "image/jpeg",
  "mission": {
    "id": 2,
    "name": "Ir de bicicleta",
    "tag": "Mobilidade",
    "description": "Troque o carro pela bicicleta em um trajeto do dia",
    "emoji": "🚲"
  },
  "callbackUrl": null
}
```

| Campo | Observação |
|---|---|
| `validationId` | Identificador da análise. **Devolva-o** se usar o webhook. |
| `imageUrl` | URL **temporária e assinada**, válida por 15 minutos. Basta um `GET` simples, sem autenticação. Analise assim que receber: o link expira. |
| `imageBase64` | Preenchido só se configurarmos o modo `BYTES`. Nesse caso `imageUrl` vem nulo. Avise se você precisar deste modo. |
| `mission` | O contexto. É contra isto que a foto deve ser avaliada. |
| `callbackUrl` | Preenchido só se o webhook estiver ligado. Nulo significa: responda na própria requisição. |

**O bucket é privado.** A URL assinada existe justamente para você ler a imagem
sem receber credencial nenhuma, e para o acesso expirar sozinho. Foto de
evidência costuma mostrar a casa e a rotina da pessoa — por isso não deixamos o
bucket aberto.

---

## 2. Resposta — o que devolver

`200 OK`, corpo:

```json
{
  "status": "APPROVED",
  "detections": [
    "Bicicleta reconhecida",
    "Trajeto urbano identificado"
  ],
  "confidence": 0.93,
  "reason": "A foto mostra uma bicicleta em uso na via",
  "provider": "vision-v2"
}
```

| Campo | Obrigatório | Observação |
|---|---|---|
| `status` | **sim** | `APPROVED` ou `REJECTED`. Ver a regra abaixo. |
| `detections` | não | O que foi reconhecido. **É mostrado ao usuário** — escreva em português e de forma compreensível, não em rótulos de modelo. |
| `confidence` | não | Entre 0 e 1. |
| `reason` | não | Justificativa curta. **Mostrada ao usuário quando reprovada** — explique o que faltou, sem acusar. |
| `provider` | não | Identificação do modelo. Ajuda a investigar depois. |

Campos extras que você acrescentar são ignorados sem quebrar nada.

### A regra que mais importa

**Só devolva `REJECTED` quando analisou e concluiu que a foto não comprova a
missão.**

Se você não conseguiu analisar — modelo fora do ar, imagem ilegível, timeout
interno, erro qualquer — **devolva erro HTTP (5xx) ou um `status` diferente dos
dois**. O Soulie trata isso como `FAILED`, que é coisa diferente de reprovação:

- `REJECTED` → o usuário é avisado de que a foto não comprova e pode tentar de novo;
- `FAILED` → o usuário é avisado de que **nós** tivemos um problema, e a evidência é reprocessada.

Tratar os dois igual puniria quem fez a ação corretamente por uma falha nossa.
Essa distinção é a parte do contrato que mais afeta a experiência real.

---

## 3. Webhook — se a sua análise demorar

Desligado por padrão. Se você precisar responder depois em vez de na própria
requisição, avise que habilitamos, e aí `callbackUrl` virá preenchido na
requisição.

`POST https://soulie.up.railway.app/webhooks/validations`

Cabeçalhos obrigatórios:

| Cabeçalho | Valor |
|---|---|
| `Content-Type` | `application/json` |
| `X-Soulie-Timestamp` | epoch em **segundos** |
| `X-Soulie-Signature` | HMAC-SHA256 em hexadecimal minúsculo |

Corpo: o mesmo da resposta, mais o `validationId`:

```json
{
  "validationId": 42,
  "status": "APPROVED",
  "detections": ["Bicicleta reconhecida"],
  "confidence": 0.93,
  "reason": "A foto mostra uma bicicleta em uso",
  "provider": "vision-v2"
}
```

### Como assinar

Assine a concatenação **`timestamp + "." + corpo`**, com o segredo que vamos
compartilhar, e mande o resultado em hexadecimal minúsculo.

```python
import hmac, hashlib, time, json, requests

segredo = "<o segredo compartilhado>"
corpo = json.dumps({"validationId": 42, "status": "APPROVED"}, separators=(",", ":"))
timestamp = str(int(time.time()))

assinatura = hmac.new(
    segredo.encode(), f"{timestamp}.{corpo}".encode(), hashlib.sha256
).hexdigest()

requests.post(
    "https://soulie.up.railway.app/webhooks/validations",
    data=corpo,
    headers={
        "Content-Type": "application/json",
        "X-Soulie-Timestamp": timestamp,
        "X-Soulie-Signature": assinatura,
    },
)
```

**Assine exatamente os bytes que você vai enviar.** Se serializar de novo depois
de assinar, espaçamento ou ordem de campos mudam e a assinatura não bate.

### Por que tudo isso

Esse endpoint **concede XP** e é público. Sem assinatura, qualquer pessoa que
descobrisse a URL aprovaria a própria evidência. O timestamp entra na assinatura
porque, sem ele, uma requisição legítima capturada valeria para sempre e poderia
ser reenviada para creditar XP de novo.

### Respostas

| Status | Significado |
|---|---|
| `204` | Aceito e processado. |
| `400` | Corpo inválido ou sem `validationId`. |
| `401` | Assinatura inválida, ausente, ou timestamp fora da janela de 5 minutos. |
| `503` | Webhook ligado sem segredo configurado do nosso lado. Avise-nos. |

Repetir a mesma chamada é seguro: o processamento é **idempotente** e não
credita XP duas vezes.

---

## 4. O que precisamos de você para ligar

1. a **URL** do seu endpoint;
2. a **chave de API**, se você exigir uma;
3. se prefere `imageUrl` (padrão) ou `imageBase64`;
4. se vai responder na própria requisição ou pelo **webhook** — e, nesse caso,
   combinamos o segredo compartilhado;
5. um tempo típico de resposta, para ajustarmos o timeout.
