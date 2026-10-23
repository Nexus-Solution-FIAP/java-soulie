# Chat da Alma

A tela Soulie IA do app chama `POST /v1/chat/soulie-ia` (o mesmo contrato em `/api/v1`). A resposta é sempre `{ "reply": "..." }` — campo que o app já consome. Nada foi removido nem renomeado.

Há duas IAs no produto, e elas não se misturam:

| Papel | Onde | Decide |
|---|---|---|
| Foto | `validacao_fotos`, `POST /v1/analisar` | se a evidência vale XP |
| Alma | este fluxo, LLM OpenAI-compatível | texto da conversa |

## Fluxo

```
App  POST /v1/chat/soulie-ia { message, context? }
  → JWT obrigatório
  → validators (mensagem obrigatória, máx. 500)
  → ChatApiService monta fatos do banco (missões abertas hoje, ranking, XP)
  → chips (missões / ranking / XP / dicas)    → resposta determinística
  → pergunta livre (ou missão da tela)        → LlmClient
       sem chave, timeout ou 4xx              → FAQ, nunca 500
  → { reply }
```

O histórico fica no `useState` do app. O Java é stateless: não há tabela de conversa.

## Provedor

O `LlmClient` fala o JSON de `POST …/chat/completions` com `Authorization: Bearer`. Groq é o padrão. Gemini entra mudando três variáveis para o endpoint OpenAI-compatível da Google. Sem SDK no `pom.xml`.

A URL vem só de config (`soulie.chat.url`). Valor vindo do body seria SSRF.

## Variáveis

| Variável | Papel |
|---|---|
| `SOULIE_CHAT_LLM_URL` | endpoint OpenAI-compatível. Vazio = só FAQ |
| `SOULIE_CHAT_LLM_API_KEY` | Bearer. Vazio = só FAQ |
| `SOULIE_CHAT_LLM_MODEL` | padrão `qwen/qwen3.8-27b` |
| `SOULIE_CHAT_LLM_READ_TIMEOUT` | padrão `PT20S` |

O `SoulieApplication` carrega `.env` da raiz em system properties, sem sobrescrever o que o SO ou o run config já definiram.

A mensagem do usuário e o bloco de fatos (missões abertas, XP, linha de ranking) saem para o provedor. O nome da conta não vai. Sem chave configurada, nada sai da máquina.

## Rate limit

`POST /**/chat/soulie-ia`: 20 req / hora por usuário autenticado. Um padrão só, para `/v1` e `/api/v1` contarem no mesmo balde.

## O que ficou de fora

- Histórico persistido
- Streaming
- Function calling
- `missionId` com validator de posse — o app ainda manda `name/desc/xp/tag`; o Java tenta casar o nome no catálogo e, se achar, usa o registro do banco
