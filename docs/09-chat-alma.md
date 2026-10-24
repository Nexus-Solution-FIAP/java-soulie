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
  → chips curtos (missões / ranking / XP / dicas)  → resposta determinística
  → pergunta livre (ou missão da tela)             → LlmClient
       system = persona + 4 guias + 24 diálogos
       user   = FATOS da conta + pergunta
       sem chave, timeout ou 4xx                   → FAQ, nunca 500
  → { reply }
```

O histórico fica no `useState` do app. O Java é stateless: não há tabela de conversa.

## Base de conhecimento

A instrução do modelo não é mais um parágrafo curto. `ChatKnowledge` lê, no startup, os Markdown em `src/main/resources/chat/`:

| Arquivo no classpath | Papel |
|---|---|
| `contexto-geral-persona.md` | identidade, tom, escopo e limites |
| `sistema-missoes.md` | missões, foto, XP, sequência, conquistas |
| `funcionalidades-do-app.md` | navegação, cadastro, coleção, comunidade, privacidade |
| `guia-de-reciclagem.md` | descarte e hábitos, com fontes brasileiras |
| `visao-do-projeto.md` | propostas e incertezas, sem tratar como regra atual |
| `exemplo-interacao.md` | 24 diálogos de tom; não são contas reais |

A origem editorial é a pasta `contexto-ia/` na raiz do workspace. Copiar de novo para `resources/chat/` quando um guia mudar. O `README.md` daquela pasta **não entra** no prompt: tem gabarito de avaliação.

Chips da tela continuam determinísticos só quando a frase é curta (até 48 caracteres). "Qual estágio com 49 XP da espécie?" vai ao LLM, para a base responder com a regra por espécie.

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
