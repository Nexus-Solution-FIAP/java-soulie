---
name: arch-docs
description: Atualiza a documentacao de arquitetura em docs/ a partir do codigo. Use depois de mudar comportamento, adicionar dominio, endpoint ou integracao externa, para a documentacao nao apodrecer.
---

# Skill: arch-docs

## Os documentos

| Arquivo | Escopo |
|---|---|
| `README.md` | o que é, como subir, variáveis de ambiente, como rodar teste, onde está o Swagger |
| `docs/01-arquitetura.md` | camadas, pacote por feature, padrão de 5 peças, as duas gerações de API, por que `Mission` e `DailyMission` coexistem |
| `docs/02-integracao-repositorios.md` | o que veio do `soulie-soul-up`, o que não veio e por quê, renumeração das migrations |
| `docs/03-fluxo-validacao-imagem.md` | upload, storage, `Evidence`/`Validation`, fila, IA, aprovação, XP, push — e cada caminho de falha |
| `docs/04-contrato-ia.md` | **documento de entrega ao parceiro**: JSON de ida e volta, assinatura HMAC, erros, webhook |
| `docs/05-cloudflare-e-rate-limit.md` | proxy, headers consumidos, resolução de IP real, tabela de limites, resposta 429 |
| `docs/06-social.md` | follow e amizade, por que amizade é derivada, como ranking/feed/nudge usam |
| `docs/07-seguranca.md` | correções aplicadas, modelo de ameaças de webhook e upload, o que ficou de fora |
| `docs/08-operacao.md` | variáveis, filas e DLQ, o que monitorar, como reprocessar validação travada |

## Como atualizar

1. Veja o que mudou: `git diff --stat` ou `git log --oneline -10`.
2. Mapeie a mudança para o documento certo. Mudança de rota toca `01`; de schema toca `01` e `02`; no fluxo de validação toca `03` e provavelmente `04`.
3. **Leia o código antes de escrever.** Documentação que descreve a intenção em vez do comportamento real é pior que documentação nenhuma.
4. Atualize também, quando couber:
   - a descrição do OpenAPI em `infra/springdoc/SpringDocConfig.java`;
   - o quadro de progresso no `plan.md` da raiz;
   - o `CLAUDE.md`, se a **convenção** mudou (não se só o código mudou).

## Estilo

- Português, direto. Frase curta.
- Explique **por que** a decisão foi tomada, não só o que o código faz — o "o quê" se lê no código.
- Use tabela para contrato e lista numerada para fluxo.
- Diagrama em bloco de texto, sem dependência de ferramenta.
- Caminho de arquivo sempre relativo à raiz do repositório.
- Registre o que **não** funciona ou ficou de fora. É a parte mais útil para quem chega depois.
