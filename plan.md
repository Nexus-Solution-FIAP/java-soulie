# Integração java-soulie + soulie-soul-up, com Cloudflare, Rate Limit, RabbitMQ, Social e Validação de Imagem por IA

> Documento vivo. Atualizado conforme as fases avançam — o quadro de progresso abaixo é a fonte de verdade do andamento.

## Progresso

| Fase | Entrega | Status |
|---|---|---|
| 0 | `CLAUDE.md`, agentes, skills, hook de segurança, `.gitignore` + `.env.example` | ✅ concluída |
| 1 | Correções de segurança 1–6, 8, 9 + perfis do Spring (`dev`/`prod`/`test`) | ⬜ não iniciada |
| 2 | Port dos domínios do `soulie-soul-up` + `@Builder` + migrations V19/V20 | ⬜ não iniciada |
| 3 | Port dos 50 testes + perfil H2 + `spring-boot-starter-test` e `h2` no pom | ⬜ não iniciada |
| 4 | Pipeline de IA: storage S3, RabbitMQ, cliente da IA, webhook HMAC, migration V21 | ⬜ não iniciada |
| 5 | Social: follow/amizade, endpoints, migration V22 | ⬜ não iniciada |
| 6 | Cloudflare + rate limit distribuído (Bucket4j/Redis) | ⬜ não iniciada |
| 7 | Documentação `docs/01`–`08` + README + revisão final de segurança | ⬜ não iniciada |

Legenda: ⬜ não iniciada · 🟡 em andamento · ✅ concluída e revisada pelo `security-auditor` · ⚠️ bloqueada

**Baseline verificado:** `./mvnw clean compile` passa na branch `feature/integracao-soulup-ia-infra` (126 classes, exit 0). Qualquer quebra a partir daqui é nossa.

### Registro de decisões e mudanças

| Data | O que mudou |
|---|---|
| 2026-10-20 | Plano inicial escrito. Decisões do usuário registradas na seção "Decisões já tomadas". Verificado que o `bucket4j-spring-boot-starter` não suporta Boot 4.1 → usar a biblioteca core com filtro próprio. |
| 2026-10-20 | **Fase 0 concluída.** Branch `feature/integracao-soulup-ia-infra` criada a partir de `feature/adding-soulie-app-endpoints`. Baseline compila. Criados `CLAUDE.md`, 3 agentes, 4 skills, hook determinístico de segurança, `.env.example`, e `.gitignore` passou a ignorar `.env`. O hook foi calibrado contra os 110 arquivos Java do projeto: **107 limpos, 3 com achado, zero falso positivo** — e os achados são exatamente os itens #3 (vazamento no `ErrorHandler`), #4 (CORS curinga) e #12 (`getOriginalFilename`) da tabela de segurança. Rodando contra o `application.yaml` ele também pega o item #5 (fallback `SenhaReserva`). |

---

## Context

O produto **Soulie** existe hoje em **dois repositórios Java Spring Boot que são forks divergentes do mesmo app**, e nenhum dos dois está completo:

| | `java-soulie` (base) | `soulie-soul-up` (fonte) |
|---|---|---|
| Pacote | `br.com.fiap.soulie` | `br.com.fiap.soulup` |
| Branch | `feature/adding-soulie-app-endpoints` | `master` |
| Boot / Java | 4.1.1 / **21** | 4.1.1 / 17 |
| Migrations | `db.migrations` V1–V18 | `db/migration` V1–V13 |
| Tem | o **contrato do app mobile** (`/api/v1/*`: perfil, missões por período, conquistas, feed, ranking, nudges, push Expo, jardim com invite code) | a **profundidade de domínio** (action, mission, evidence, validation, reward, history_points), a **cadeia de validators** e **50 arquivos de teste** |
| Não tem | Action, Mission, Evidence, Validation, Reward, HistoryPoints, validators, testes (1 teste trivial) | todo o contrato `/v1`, achievements, feed, notificações, push |

Os dois compartilham o mesmo ancestral: **V1–V11 são o mesmo schema** (só diferem em espaços em branco). A partir da V12 divergiram.

Três fatos que definem o trabalho:

1. **`java-soulie` já criou as tabelas `actions`, `missions`, `evidences`, `validations`, `history_points` e `rewards` (migrations V4–V11) mas apagou as entidades JPA correspondentes.** As tabelas estão órfãs. Portar os domínios do `soulie-soul-up` é em grande parte **re-conectar entidades a tabelas que já existem**, não criar schema novo.
2. **`POST /api/v1/missions/{id}/validate` já recebe `MultipartFile photo` e já devolve um campo `aiDetections`** — mas a implementação **joga os bytes da foto fora** (guarda só `getOriginalFilename()`) e **dá o XP na hora, sem validar nada**. O contrato do app mobile já espera a IA; a implementação é um stub. É também uma falha de integridade: dá para ganhar XP sem foto.
3. **No `soulie-soul-up` o fluxo de aprovação foi desenhado para uma IA externa que nunca existiu.** Os comentários no código dizem: *"a evidência fica PENDING até a IA externa validar"* e *"quem aprova/reprova é a IA externa"*. Como nada nunca grava `APROVED`, o `ApprovedEvidenceValidator` bloqueia e **`PUT /mission/{id}/complete` é impossível de concluir hoje**.

Ou seja: a validação de imagem por IA que o parceiro de equipe pediu é exatamente a peça que **os dois repositórios já esperam** e nenhum implementou. Ela é o eixo que une a integração às features novas.

### Resultado pretendido

Um único repositório (`java-soulie`, pacote `br.com.fiap.soulie`) que:

- preserva 100% do contrato `/api/v1/*` que o app mobile do parceiro consome — **nada quebra**;
- recupera os domínios, a cadeia de validators e a suíte de testes do `soulie-soul-up`;
- tem um pipeline real de **evidência → fila → IA externa → aprovação → recompensa**;
- está protegido por **Cloudflare (WAF/proxy)** + **rate limit distribuído (Bucket4j + Redis)**;
- tem **seguir/amizade** como grafo social de verdade, além do jardim;
- vem com **documentação de arquitetura** explicando cada parte e como elas conversam;
- vem com **agentes e skills do Claude** no repositório, para que o trabalho futuro custe menos tokens e passe por revisão de segurança rigorosa.

### Decisões já tomadas pelo usuário

1. **Direção do merge**: `java-soulie` é a base e mantém o pacote `br.com.fiap.soulie`. O código do `soulie-soul-up` é portado para dentro dele. Meta: juntar sem perder nada e sem corromper.
2. **Cloudflare = WAF/proxy reverso na frente da API.** O app precisa confiar no proxy e resolver o IP real via `CF-Connecting-IP` (sem isso o rate limit estrangula o próprio proxy). Não é R2, não é Workers AI, não é rate limit só na edge.
3. **Storage de imagem = bucket do Railway, compatível com S3** (access key / secret / endpoint). Implementação única que serve Railway, MinIO, R2 e S3 trocando variável de ambiente.
4. **A IA não é nossa responsabilidade.** Só preparamos o código para enviar a imagem e esperar o retorno `{image, status}`. O worker do RabbitMQ chama a API da IA e lê a resposta ali mesmo; a fila é que dá o assíncrono, o retry e a DLQ. Além disso deixamos pronto um **webhook de entrada assinado (HMAC)** caso a IA do parceiro seja do tipo que responde depois. O contrato (JSON de ida e volta + assinatura) tem que ser documentado para entregar ao parceiro.
5. **Fluxo de validação assíncrono puro**: guarda a foto → cria `Evidence` + `Validation(PENDING)` → publica na fila → responde **202 com o `validationId`**. O XP só é creditado quando a IA aprovar, e aí vai push.
6. **Rate limit distribuído com Redis** (Bucket4j + Redis), porque a intenção é escalar horizontalmente.
7. **Social: seguir unilateral + amizade automática quando mútuo.** Seguir é imediato, sem tela de aceite. Follow mútuo ⇒ "amigos", e amizade libera interações e feed.

---

### Índice das partes

Integração (1) · Agentes e skills (2) · Social (3) · Pipeline de IA (4) · Cloudflare (5) · Rate limit (6) · Segurança (7) · Documentação (8). **As partes 7 e 8 aparecem antes das 4–6 no arquivo**; as referências cruzadas usam o número, não a posição.

---

## Descobertas que mudam o plano

Levantei isto diretamente nos dois repositórios — não são suposições:

**1. Zero caminhos de arquivo em comum.** Os 130 arquivos do `soulie-soul-up` e os 163 do `java-soulie` não colidem em disco, porque os pacotes base diferem (`soulup` vs `soulie`). Então **não existe conflito de merge no Git**. O trabalho é de **portar código**, não de resolver conflitos.

**2. Colisão de rotas é o risco real de arranque.** Portar os controllers do `soulie-soul-up` como estão derruba a aplicação no boot com *ambiguous mapping*:

| Rota | Dono no `java-soulie` | O que o `soulie-soul-up` traria | Ação |
|---|---|---|---|
| `/login` | `AuthController` | `AuthenticationController` | **não portar** o controller |
| `/user` | `UserController` | `UserController` | **não portar** |
| `/avatar` | `AvatarController` | `AvatarController` | **não portar** |
| `/garden` | `GardenController` | `GardenController` | **não portar** |
| `/action`, `/mission`, `/evidence`, `/reward`, `/history-points` | — | 5 controllers | **portar** |

**3. Colisão de tabela em `interactions`.** O `java-soulie` usa as colunas `kind` + `created_at` (log de nudges); o `soulie-soul-up` usa `type`. Duas entidades `Interaction` na mesma tabela corrompem o mapeamento. Decisão: **não portar** `domain/interaction/` do `soulie-soul-up`; em vez disso mapear `TypeInteraction{WATERING, INCENTIVE, VISIT}` para a coluna `kind` que já existe.

**4. Os testes portados já são nativos do Boot 4.** O `RepositoryTestBase` do `soulie-soul-up` importa `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase` e `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager` — os pacotes novos do Boot 4. Não há migração de API de teste a fazer.

**5. Mas os testes dependem de `@Builder`, que o `java-soulie` não tem em nenhuma entidade.** `TestData` e `RepositoryTestBase` constroem tudo com `User.builder()`, `Avatar.builder()`, `Action.builder()`, `Garden.builder()`. Adicionar `@Builder` nas entidades do `java-soulie` é pré-requisito do port dos testes.

**6. Docker não está disponível nesta máquina** (`docker --version` falha). Isso descarta Testcontainers como estratégia principal. O caminho é o do `soulie-soul-up`: **H2 em memória** com `flyway.enabled=false` + `ddl-auto=create-drop` + `globally_quoted_identifiers=true` (necessário porque `level` é palavra reservada no H2).

**7. `src/main/resources/db/soulie-schema.sql` está desatualizado e é uma segunda fonte de verdade.** O próprio cabeçalho diz "o Flyway aplica db.migrations/V1 até V12" quando o projeto já está na V18. Precisa ser regenerado ou removido.

**8. Não existe nenhuma infraestrutura de projeto**: sem README, sem `CLAUDE.md`, sem `.claude/`, sem `.github/`, sem Dockerfile, sem `docker-compose`, sem `.env.example`, sem perfis do Spring (`application.yaml` único), e o `.gitignore` **não ignora `.env`** — o que é perigoso agora que vamos introduzir chaves de S3, Redis, RabbitMQ, HMAC e da IA.

---

## Parte 1 — Integração dos dois repositórios

Base: `java-soulie`, pacote `br.com.fiap.soulie`, Java 21. Branch de trabalho nova a partir de `feature/adding-soulie-app-endpoints` (que está 6 commits à frente de `develop` e 0 atrás — ponto de partida limpo).

### 1.1 O que portar do `soulie-soul-up`

As tabelas `actions`, `missions`, `evidences`, `validations`, `history_points` e `rewards` **já existem** no `java-soulie` (migrations V4–V11) sem entidades JPA. Portar é reconectar entidades a tabelas órfãs.

| Origem (`br.com.fiap.soulup`) | Destino (`br.com.fiap.soulie`) | Observação |
|---|---|---|
| `domain/action/**` | `domain/action/**` | `dto/` já é a convenção; manter |
| `domain/mission/{Mission,MissionMapper,MissionRepository,MissionService,enums/,record/}` | `domain/mission/**` | conviver com `DailyMission`; `record/` → `dto/` |
| `domain/evidence/**` | `domain/evidence/**` | `record/` → `dto/` |
| `domain/validation/**` | `domain/validation/**` | `record/` → `dto/` |
| `domain/reward/**` | `domain/reward/**` | `record/` → `dto/` |
| `domain/history_points/**` | `domain/history_points/**` | `record/` → `dto/` |
| `validations/**` (6 interfaces + 22 `@Component`) | `validations/**` | a cadeia de validators, intacta |
| `infra/exception/BusinessException` | `infra/exception/BusinessException` | hoje é **classe vazia**; implementar de verdade e tratar no `ErrorHandler` |
| `controller/{Action,Mission,Evidence,Reward,HistoryPoints}Controller` | `controller/**` | os 5 sem colisão de rota |
| `src/test/**` (50 arquivos, ~3.060 linhas) | `src/test/**` | ver 1.3 |
| `src/test/resources/application-test.yaml` | idem | perfil H2 |

**Não portar**: `AuthenticationController`, `UserController`, `AvatarController`, `GardenController` (colisão de rota), `domain/interaction/**` (colisão de tabela), `domain/garden/**`, `domain/user/**`, `domain/avatar/**` (as versões do `java-soulie` são estritamente mais novas), `SecurityConfigurations`, `SpringDocConfig`, `ErrorHandler` (já existem).

**Aproveitar do `soulie-soul-up` sem portar o arquivo**: a lista explícita de origens de CORS (`${CORS_ALLOWED_ORIGINS}`) — o `java-soulie` hoje libera `setAllowedOriginPatterns(List.of("*"))` com todos os headers, e isso precisa ser fechado.

### 1.2 Arquivos a modificar no `java-soulie`

- `src/main/java/br/com/fiap/soulie/domain/user/User.java` — adicionar `@Builder` (+ `@Builder.Default` nos campos com default) e `spendPoints(int)` (existe no `soulie-soul-up`, necessário para `Reward`). Manter `isEnabled()` como está: o `java-soulie` respeita `active`, o `soulie-soul-up` devolvia `true` sempre e por isso deixava usuário excluído continuar logando.
- `domain/avatar/Avatar.java`, `domain/garden/Garden.java` — adicionar `@Builder`. Em `Garden`, `inviteCode` é `nullable=false, unique=true`: os helpers de teste precisam preencher.
- `domain/mission/*` — conviver com `Mission` (legado, `action`+`avatar`+`status`) e `DailyMission` (catálogo do app). São tabelas distintas (`missions` vs `daily_missions`), então coexistem sem corromper nada.
- `infra/exception/ErrorHandler.java` — tratar `BusinessException`; **parar de devolver `ex.getLocalizedMessage()`** no handler de `Exception` (vazamento de informação).
- `infra/security/SecurityConfig.java` — CORS por allowlist; registrar o filtro de rate limit.
- `infra/security/SecurityFilter.java` — hoje faz NPE quando o token é válido mas o usuário foi excluído, e devolve 500 em vez de 401 quando o token é inválido (o `ErrorHandler` não alcança exceções de filtro).
- `src/main/resources/application.yaml` — virar perfis (`application.yaml` + `-dev` + `-prod`); corrigir `server.error.include-stacktracer` (**está com typo**, é `include-stacktrace`, então hoje não faz nada).
- `src/main/resources/db/soulie-schema.sql` — regenerar ou remover.
- `.gitignore` — adicionar `.env`.

### 1.3 Estratégia de testes (o maior gerador de esforço)

Portar os 50 arquivos exige, em ordem:
1. renomear pacote `br.com.fiap.soulup` → `br.com.fiap.soulie` e `record` → `dto`;
2. `@Builder` nas entidades do `java-soulie` (item 1.2);
3. `ControllerTestBase` importa `SecurityConfigurations` → trocar por `SecurityConfig`;
4. criar `src/test/resources/application-test.yaml` (H2, `flyway.enabled=false`, `ddl-auto=create-drop`, `globally_quoted_identifiers=true`) — hoje **não existe diretório de test resources** no `java-soulie`;
5. ajustar helpers para os campos novos obrigatórios (`Garden.inviteCode`, e os defaults de `User` que vêm de `@PrePersist`);
6. adicionar `spring-boot-starter-test` e `h2` (escopo test) ao `pom.xml` — o `java-soulie` **removeu os dois**.

O `SoulieApplicationTests.contextLoads()` atual exige um Postgres real e as variáveis `DB_*`; passa a rodar no perfil `test`.

Testcontainers fica **fora de escopo** (sem Docker local). Em troca, as migrations não são exercitadas por teste — risco aceito e registrado na documentação, com a recomendação de rodar `flyway:migrate` contra um Postgres descartável no CI.

---

## Parte 2 — Infraestrutura de agentes e skills (economia de tokens + IA de segurança)

Isto entra **primeiro**, antes do código, porque é o que faz todas as fases seguintes custarem menos. Hoje o `java-soulie` não tem nada: sem `CLAUDE.md`, sem `.claude/`, sem README. Cada sessão futura re-descobre o projeto do zero — é exatamente o gasto de tokens que você quer cortar.

### 2.1 `CLAUDE.md` na raiz — o maior ganho isolado

Documenta as convenções que eu tive que descobrir lendo o código, para ninguém precisar descobrir de novo:
- stack (Boot **4.1.1**, Java **21**, Postgres, Flyway em `classpath:db.migrations` — **com ponto, não barra**);
- o prefixo duplo `/api/v1` + `/v1` em todo controller novo;
- o padrão de 5 peças por domínio (`Entity` + `Repository` + `Service` + `Mapper` MapStruct + `dto/` records);
- usuário logado sempre via `@AuthenticationPrincipal User user` com `@Parameter(hidden = true)`;
- a cadeia de validators (`List<XValidator>` injetada e iterada);
- regra de migration: próximo número, nomenclatura `V<n>__snake_case.sql`, sintaxe só Postgres;
- testes: perfil `test` com H2, `@Builder`, as classes-base;
- comentários, mensagens e Swagger em **português**;
- **regra dura: não alterar o contrato `/v1` sem avisar** — o app do parceiro depende dele;
- as armadilhas conhecidas (nomes de starter no Boot 4, palavras reservadas do H2, o `soulie-schema.sql` que desatualiza).

### 2.2 Agentes (`.claude/agents/`)

Três, com modelos escolhidos por custo:

| Agente | Modelo | Papel |
|---|---|---|
| `security-auditor` | **opus** (rigor) | Revisa todo diff contra um checklist severo: IDOR (o endpoint confere se o recurso é do `@AuthenticationPrincipal`?), segredo em código ou log, injeção em `@Query`, SSRF no cliente da IA e no webhook, comparação de HMAC em tempo constante, path traversal na chave do S3, validação de content-type e magic bytes da imagem, limite de tamanho, CORS, vazamento de erro, tratamento do JWT, **bypass de rate limit por header falsificado** (crítico com Cloudflare na frente), DoS por retry infinito na fila, PII em log. Entrega achados com severidade e `arquivo:linha`. |
| `spring-domain-builder` | sonnet | Implementa uma fatia de domínio já no padrão da casa, lendo as convenções do `CLAUDE.md` em vez de re-derivá-las. |
| `soulie-test-writer` | sonnet | Escreve e porta testes contra o harness H2 (`RepositoryTestBase`, `ControllerTestBase`, `TestData`). |

### 2.3 Skills (`.claude/skills/`)

| Skill | Para que serve |
|---|---|
| `migration` | Cria a próxima migration corretamente: calcula o número, respeita a pasta `db.migrations`, usa só sintaxe Postgres, lembra do impacto no perfil H2 e atualiza o `soulie-schema.sql`. |
| `endpoint` | Adiciona endpoint no padrão: prefixo duplo, principal oculto no Swagger, `@Operation` em pt-BR, record em `api/dto`, mapeamento no `ErrorHandler`, bucket de rate limit e teste de segurança. |
| `arch-docs` | Mantém a documentação de arquitetura (Parte 8) viva a partir do código, em vez de apodrecer. |

O `/resolve` do `soulie-soul-up` também vem junto, já que é útil e já existe.

### 2.4 A IA de segurança: dois níveis

Rodar uma IA a cada edição seria lento e caro. A divisão que respeita o pedido sem queimar tokens:

1. **Sempre, de graça: hook determinístico.** Um `PostToolUse` em `Edit|Write` de `**/*.java` roda um script (não IA) que procura um conjunto pequeno de erros duros — segredo hardcoded, `printStackTrace()`, `getLocalizedMessage()` dentro de corpo de resposta, `permitAll()` novo, `@CrossOrigin`, `Math.random()` gerando token, método `@*Mapping` sem `@AuthenticationPrincipal`. Custo zero de token, resposta instantânea.
2. **Nos portões de fase: o `security-auditor` (opus).** Revisão com raciocínio de verdade sobre authz, SSRF, HMAC e abuso de fila. Roda ao fim de cada fase, não a cada arquivo.

O `/security-review` nativo do Claude Code continua disponível como terceira camada antes do PR.

---

## Parte 3 — Seguir e amizade

Hoje o "social" do `java-soulie` é **mediado só pelo jardim**: `ranking` usa `gardenService.members(current)`, `feed` é escopado por `garden_id`, e `nudge` exige que o alvo esteja no mesmo jardim. O código já chama essas pessoas de amigos (`NUDGES_PER_FRIEND_PER_DAY = 3`, `@Operation(summary = "Lembra um amigo da sequência")`) — mas **não existe nenhum grafo social**: sem tabela `follows`, sem `Friendship`, sem auto-relacionamento em `User`.

### 3.1 Modelo

Novo pacote `domain/social/`:

```
Follow { id, follower_id → users, followee_id → users, created_at }
   UNIQUE (follower_id, followee_id)
   CHECK  (follower_id <> followee_id)
```

Amizade **não é tabela** — é a propriedade derivada "existem os dois sentidos do follow". Isso evita um segundo estado para manter sincronizado e dá a semântica pedida (seguir unilateral imediato; amigo quando mútuo).

`FollowRepository` com as consultas que sustentam tudo:
- `existsByFollower_IdAndFollowee_Id` (já sigo?)
- `countByFollowee_Id` / `countByFollower_Id` (seguidores / seguindo)
- uma `@Query` de amizade mútua (`EXISTS` nos dois sentidos) e uma que lista os ids dos amigos — é a consulta quente, usada por ranking, feed e nudge

`FollowService` + `FriendshipService` expõem `isFriend(a, b)`, `friendIds(user)`, `follow`, `unfollow`.

### 3.2 Endpoints novos (`/api/v1/social` + `/v1/social`)

| Método | Rota | Papel |
|---|---|---|
| POST | `/follow/{userId}` | segue; devolve se virou amizade mútua |
| DELETE | `/follow/{userId}` | deixa de seguir (desfaz a amizade por consequência) |
| GET | `/followers` | quem me segue |
| GET | `/following` | quem eu sigo |
| GET | `/friends` | follow mútuo |
| GET | `/users/search?q=` | busca por `handle`/nome para poder seguir |

Respeitando o que já existe: `User.publicName()` já honra `showFullName`, e `User.handle` já é único (`uq_users_handle`) — a busca usa os dois, sem expor `email`.

### 3.3 Integração com o que já funciona — sem quebrar

- **`nudge`**: hoje exige mesmo jardim. Passa a aceitar **mesmo jardim OU amizade mútua**. Amplia sem remover comportamento. A cota de 3/dia e o `allowsNudges()` continuam valendo.
- **`ranking` e `feed`**: continuam escopados no jardim por padrão (não mexer no que o app já consome). Ganham um parâmetro opcional `scope=garden|friends`, default `garden`. Contrato preservado.
- `SharedGardenValidator` (vindo do `soulie-soul-up`) ganha um irmão `FriendshipValidator` na mesma cadeia de validators, em vez de ter a regra espalhada em `if`.

### 3.4 Privacidade e abuso

Follow é um vetor clássico de abuso, então entra com freio desde o início: rate limit próprio no `POST /follow` (ver Parte 5), bloqueio de self-follow no banco (`CHECK`) e no validator, e `active=false` não pode ser seguido nem aparecer na busca.

---

## Parte 7 — Correções de segurança já identificadas

Encontrei estas lendo o código. Não são hipotéticas e várias são pré-requisito das features novas (não dá para pôr rate limit em cima de um resolvedor de IP que o Cloudflare falsifica, nem webhook sem HMAC correto).

| # | Onde | Problema | Correção |
|---|---|---|---|
| 1 | `infra/security/SecurityFilter.java` | Token válido de usuário **excluído** → `userRepository.findByEmail` devolve `null` → `user.getAuthorities()` dá **NPE** | checar null e seguir sem autenticar |
| 2 | `infra/security/SecurityFilter.java` | `tokenService.getSubject` lança `RuntimeException` em token inválido/expirado; exceção de filtro **não passa** pelo `@RestControllerAdvice` → responde **500 em vez de 401** | tratar no filtro e devolver 401 via `AuthenticationEntryPoint` |
| 3 | `infra/exception/ErrorHandler.java` | `treatmentInternalServerError` devolve `ex.getLocalizedMessage()` no corpo → **vazamento de informação** (stack, SQL, nome de tabela) | logar completo no servidor, devolver mensagem genérica + id de correlação |
| 4 | `infra/security/SecurityConfig.java` | CORS com `setAllowedOriginPatterns(List.of("*"))` e `setAllowedHeaders(List.of("*"))` | allowlist via `${CORS_ALLOWED_ORIGINS}`, como o `soulie-soul-up` já fazia |
| 5 | `application.yaml` | `SECRET_JWT` com fallback **`"SenhaReserva"`** — se a variável faltar em produção, sobe com segredo público e previsível | sem fallback; falhar o boot se ausente no perfil `prod` |
| 6 | `application.yaml` | `server.error.include-stacktracer` está **com typo** (é `include-stacktrace`), então a configuração **não faz nada** hoje | corrigir a chave |
| 7 | `domain/mission/MissionApiService.java:70` | `POST /v1/missions/{id}/validate` **credita XP sem foto nenhuma** e descarta os bytes da que vier | resolvido pela Parte 6 (XP só após aprovação da IA) |
| 8 | `infra/security/TokenService.java` | expiração de 2h com `ZoneOffset` fixo em `-03:00`; sem refresh e sem revogação | usar UTC; registrar a ausência de refresh token como decisão consciente na documentação |
| 9 | `.gitignore` | não ignora `.env`, e vamos introduzir chaves de S3, Redis, RabbitMQ, HMAC e da IA | adicionar `.env` e criar `.env.example` sem valores |
| 10 | Novo (Parte 5) | atrás do Cloudflare, `request.getRemoteAddr()` é o **IP do proxy** — rate limit por IP estrangularia todo mundo junto; e `CF-Connecting-IP` é **falsificável** se aceito de qualquer origem | só confiar no header quando a conexão vier de faixa de IP do Cloudflare; senão usar o IP do socket |
| 11 | Novo (Parte 6) | webhook da IA é endpoint público que concede XP | HMAC com comparação em **tempo constante**, timestamp para anti-replay, e idempotência por `validationId` |
| 12 | Novo (Parte 6) | upload de imagem aceito em endpoint autenticado | validar **magic bytes** (não só `Content-Type`), limite de 10MB já configurado, e chave do S3 gerada pelo servidor (UUID) para evitar path traversal |

---

## Parte 8 — Documentação (entregável final)

Hoje a única documentação do `java-soulie` é o Swagger, e o texto do `SpringDocConfig` já é bom. A documentação nova vai em `docs/` e **explica como cada parte funciona e conversa**, que foi o pedido:

| Arquivo | Conteúdo |
|---|---|
| `README.md` | o que é, como subir (variáveis de ambiente, Postgres, Redis, RabbitMQ), como rodar os testes, onde está o Swagger |
| `docs/01-arquitetura.md` | o mapa: camadas, pacote por feature, o padrão de 5 peças, por que existem duas gerações de API (`/user` legado vs `/v1`), e por que `Mission` e `DailyMission` coexistem |
| `docs/02-integracao-repositorios.md` | o registro do merge: o que veio do `soulie-soul-up`, o que não veio **e por quê** (colisão de rota, colisão de tabela), e o mapa de renumeração das migrations |
| `docs/03-fluxo-validacao-imagem.md` | o fluxo ponta a ponta com diagrama: upload → S3 → `Evidence`+`Validation(PENDING)` → RabbitMQ → worker → IA → aprovação → XP → push. Inclui o que acontece em cada falha (DLQ, retry, timeout) |
| `docs/04-contrato-ia.md` | **o documento para entregar ao seu parceiro**: JSON de requisição, JSON de resposta, o cabeçalho de assinatura HMAC, como calcular, códigos de erro, e o contrato do webhook alternativo |
| `docs/05-cloudflare-e-rate-limit.md` | como pôr o Cloudflare na frente, quais headers a API consome, como o IP real é resolvido, a tabela de limites por rota, e o que o cliente recebe ao ser limitado (429 + `Retry-After`) |
| `docs/06-social.md` | o modelo de follow/amizade, por que amizade é derivada e não tabela, e como ranking/feed/nudge passam a usá-la |
| `docs/07-seguranca.md` | o que foi corrigido (tabela da Parte 7), o modelo de ameaças do webhook e do upload, e o que ficou consciente de fora (sem refresh token, migrations não testadas) |
| `docs/08-operacao.md` | variáveis de ambiente, filas e DLQ, o que monitorar, como reprocessar uma validação travada |

Mantida viva pela skill `arch-docs`. O `SpringDocConfig` também é atualizado para descrever o fluxo assíncrono novo.

---

## Parte 4 — Pipeline de validação de imagem por IA (o eixo do trabalho)

### 4.1 O que já existe e o que está faltando

- `POST /api/v1/missions/{id}/validate` **já** recebe `@RequestPart MultipartFile photo` e **já** devolve `MissionValidationResponse` com `List<String> aiDetections`. O `multipart` já está configurado (10MB/12MB).
- `MissionApiService.validate()` guarda apenas `photo.getOriginalFilename()` e **descarta os bytes**, creditando XP na hora.
- No `soulie-soul-up`, `Evidence` + `Validation` + `ResultValidation{PENDING, APROVED, REJECT}` existem com os comentários *"a evidência fica PENDING até a IA externa validar"* e *"quem aprova/reprova é a IA externa"*. Nada nunca grava `APROVED`, então `ApprovedEvidenceValidator` trava e `PUT /mission/{id}/complete` **é impossível hoje**.

Conclusão: a IA é a peça que **os dois lados já esperam**. Um pipeline só atende aos dois.

### 4.2 Modelo unificado

Uma só tabela de evidência e uma só de validação servem os **dois** modelos de missão:

```
evidences   (já existe, V6 — estender)
  id, user_id, mission_id → missions (NULL), daily_mission_id → daily_missions (NULL),
  image_url, image_key, content_type, size_bytes, submitted_at
  CHECK (exatamente um dos dois FKs preenchido)

validations  (já existe, V7/V13 — estender)
  id, evidence_id UNIQUE, result, detections, confidence, reason,
  provider, requested_at, resolved_at, attempts
```

Assim: o worker grava `APPROVED` → **destrava o `/mission/{id}/complete` legado** (via `mission_id`) **e** credita a recompensa do `/v1` (via `daily_mission_id`). Um pipeline, dois modelos de missão alimentando-o.

Sobre o enum: o `soulie-soul-up` escreve `APROVED` (erro de grafia) e a **V13 dele gravou esse literal no banco**. A migration nova normaliza para `APPROVED`/`REJECTED` e o `ResultValidation` passa a ter a grafia correta — corrigir agora, enquanto não há dado de produção dependendo disso, é mais barato que conviver.

### 4.3 O fluxo

```
app ──POST /v1/missions/{id}/validate (multipart)
      │ valida magic bytes + tamanho + missão não feita no período
      │ sobe a imagem no bucket S3-compatível (chave = UUID gerado pelo servidor)
      │ cria Evidence + Validation(PENDING)
      │ publica {validationId} na exchange
      └──► 202 Accepted  { validationId, status: "PENDING", ... }

worker (@RabbitListener)
      │ ImageValidationClient.validate(imageUrl, contexto da missão)
      │   POST na API da IA do parceiro, lê {status, detections, confidence}
      ├─ aprovado  → Validation(APPROVED) + credita XP/pontos/impacto/streak
      │              + UserActivity + AchievementApiService.sync + push Expo
      ├─ reprovado → Validation(REJECTED) + notificação explicando
      └─ erro      → retry com backoff; esgotado → DLQ + Validation(FAILED)

(alternativa, se a IA do parceiro responder depois)
IA ──POST /webhooks/validations  (HMAC assinado) ──► mesmo tratamento
```

O contexto enviado à IA vem de graça do `DailyMission`, que já tem `name`, `tag`, `description` e `emoji` — dá para perguntar "esta foto comprova *Andar de bicicleta*?" sem inventar metadado.

### 4.4 Como não quebrar o app do parceiro

A mudança de "200 com `xpGained`" para "202, XP depois" é a única quebra real. O desenho menos agressivo:

- **Nenhum campo de `MissionValidationResponse` é removido.** Entram dois novos: `validationId` e `status`. Um cliente antigo continua desserializando e lê `xpGained: 0`, `success: false`, `aiDetections: []` — não estoura.
- O status passa a **202** em vez de 200. Cliente que só checa `response.ok` continua funcionando.
- Novo `GET /v1/missions/validations/{id}` devolve o mesmo `MissionValidationResponse` já preenchido quando a IA terminar — é o endpoint de consulta.
- Quando aprovar, vai **push Expo** (o `ExpoPushService` já existe e já é usado em nudge), então o app pode reagir sem polling.
- Fica configurável um modo `soulie.validation.mode: async|sync` para o parceiro poder voltar ao comportamento imediato durante a transição dele.

### 4.5 Classes novas

| Pacote | Classes |
|---|---|
| `infra/storage/` | `ImageStorage` (porta), `S3CompatibleImageStorage` (AWS SDK v2 com `endpointOverride` + path-style → serve Railway, MinIO, R2, S3), `StoredImage` |
| `infra/ai/` | `ImageValidationClient` (no padrão `RestClient` do `ExpoPushService`), `ImageValidationRequest`/`ImageValidationResult`, `HmacSignature` (comparação em tempo constante), `ValidationWebhookController` |
| `infra/messaging/` | `RabbitConfig` (exchange, fila, DLQ, TTL), `ValidationPublisher`, `ValidationConsumer` |
| `domain/evidence/` | estendido: `EvidenceService.submitForDailyMission(...)` |
| `domain/validation/` | estendido: `ValidationService.resolve(validationId, result)` com idempotência |

O `ImageValidationClient` segue o molde do `ExpoPushService` (`RestClient` + `JdkClientHttpRequestFactory` + timeouts explícitos + records aninhados), para o projeto ter **um** jeito de fazer chamada HTTP, não dois.

---

## Parte 5 — Cloudflare (WAF / proxy reverso)

O Cloudflare é configuração externa; do lado do Java o que importa é **a API passar a entender que está atrás de um proxy**. Sem isso o rate limit da Parte 5 não funciona — ele veria o IP do Cloudflare em toda requisição e limitaria todos os usuários no mesmo bucket.

Novo pacote `infra/cloudflare/`:

- **`ClientIpResolver`** — resolve o IP real do cliente. Lê `CF-Connecting-IP`, **mas só confia nele quando a conexão vem de uma faixa de IP do Cloudflare**; caso contrário usa o IP do socket. Essa condição é o ponto todo: aceitar o header de qualquer origem permitiria a qualquer um burlar o rate limit mandando um `CF-Connecting-IP` falso por requisição. As faixas ficam em configuração (`soulie.cloudflare.trusted-ips`), com os prefixos publicados pelo Cloudflare como default.
- **`ForwardedHeaderFilter`** registrado como bean — faz o Spring montar URLs absolutas corretas (`Location`, links do Swagger) a partir de `X-Forwarded-Proto`/`Host`, em vez de devolver `http://` interno.
- **`CloudflareAccessTokenVerifier`** (opcional, desligado por default) — valida o JWT do Cloudflare Access para proteger rotas administrativas e o Swagger em produção.

Configuração nova em `application.yaml`:
```
soulie.cloudflare.enabled, .trusted-ips, .access.enabled, .access.team-domain, .access.audience
server.forward-headers-strategy: framework
```

E a documentação (`docs/05`) com o passo a passo do lado do painel: DNS proxied, SSL full strict, regras de WAF, e o aviso de que o rate limit da edge é **complemento**, não substituto — se alguém descobrir o endereço de origem no Railway, só o Bucket4j segura.

---

## Parte 6 — Rate limit distribuído (Bucket4j + Redis)

Decisão do usuário: **distribuído**, para escalar horizontalmente. Hoje não existe nada disso — o único limite do projeto é de negócio (`NUDGES_PER_FRIEND_PER_DAY = 3` contado no banco em `CommunityApiService`).

Novo pacote `infra/ratelimit/`:

- **`RateLimitFilter`** (`OncePerRequestFilter`, registrado antes do `SecurityFilter` para o login já entrar protegido) — escolhe o bucket pela rota + identidade e responde **429** com `Retry-After` e `X-RateLimit-Remaining` quando estoura.
- **`RateLimitKeyResolver`** — chave = `userId` quando autenticado (mais justo e não pune NAT compartilhado), senão o IP real vindo do `ClientIpResolver` da Parte 4.
- **`RateLimitPolicy`** — as faixas, declaradas em configuração e não em código:

| Rota | Limite | Motivo |
|---|---|---|
| `POST /login` | 5/min por IP + backoff | força bruta de senha |
| `POST /user` (cadastro, público) | 3/hora por IP | criação em massa de conta |
| `POST /v1/missions/{id}/validate` | 10/hora por usuário | upload + chamada paga de IA |
| `POST /v1/social/follow/{id}` | 30/hora por usuário | follow spam |
| `POST /v1/community/feed` e comentários | 20/hora por usuário | spam no feed |
| `POST /webhooks/validations` | por IP, estreito | endpoint público |
| default autenticado | 120/min | teto geral |

O `ErrorHandler` ganha o tratamento do 429 em JSON no mesmo formato dos outros erros.

**Nota de infraestrutura:** isto adiciona **Redis** e **RabbitMQ** como dependências de runtime no Railway. A aplicação precisa subir e funcionar mesmo se eles estiverem fora — então: se o Redis cair, o rate limit degrada para permitir (fail-open, com log de alerta) em vez de derrubar a API; se o RabbitMQ cair, o `POST /validate` devolve 503 com mensagem clara em vez de perder a evidência silenciosamente. Ambos entram na documentação de operação.

---

## Dependências novas no `pom.xml`

O projeto está em **Spring Boot 4.1.1**, que renomeou starters (`-web` → `-webmvc`) e dividiu os de teste. Então nenhuma coordenada aqui é por hábito — as de risco foram verificadas.

| Dependência | Coordenada | Fase | Observação |
|---|---|---|---|
| Testes | `spring-boot-starter-test` + `com.h2database:h2` (test) | 3 | o `java-soulie` **removeu os dois**; o `soulie-soul-up` os tem. Pré-requisito do port dos testes. |
| RabbitMQ | `spring-boot-starter-amqp` | 4 | **confirmar o nome no Boot 4.1** antes de escrever o pom |
| Redis | `spring-boot-starter-data-redis` | 6 | idem; traz Lettuce como cliente padrão |
| Rate limit | `com.bucket4j:bucket4j_jdk17-core` + `com.bucket4j:bucket4j_jdk17-lettuce` (**8.20.0**) | 6 | ver nota abaixo |
| Storage | `software.amazon.awssdk:s3` | 4 | com `endpointOverride` + path-style access |

**Decisão importante sobre o rate limit:** **não** usar o `bucket4j-spring-boot-starter` (o de autoconfiguração, `com.giffing.bucket4j.spring.boot.starter`). A última release dele é a 0.14.0 contra **Spring Boot 4.0.3**, e não há release confirmada para **4.1.x** — a issue de compatibilidade com Boot 4 segue aberta no repositório. Usar a biblioteca **core** (`bucket4j_jdk17-core`) com o módulo Lettuce e o nosso próprio `OncePerRequestFilter` é o caminho seguro, e de todo jeito é o que precisamos, porque os limites são por rota e por identidade (usuário autenticado vs IP real do Cloudflare) — regra que a autoconfiguração não expressaria bem.

> **Primeira tarefa das Fases 4 e 6 — conferir antes de escrever o pom.** É mais rápido verificar que depurar um build quebrado:
> 1. os nomes de `spring-boot-starter-amqp` e `spring-boot-starter-data-redis` no Boot 4.1;
> 2. a versão alinhada dos módulos do Bucket4j (core aparece em 8.19.0, Lettuce e Jedis em 8.20.0 no Maven Central);
> 3. se o Boot 4.1 gerencia a versão do AWS SDK v2 (se não, fixar explicitamente);
> 4. o que o Boot 4.1 oferece nativamente para confiança em proxy — se já houver filtro de IP confiável pronto, usá-lo em vez de escrever o `ClientIpResolver` à mão.

Fontes da verificação: [bucket4j-spring-boot-starter — releases](https://github.com/MarcGiffing/bucket4j-spring-boot-starter/releases), [discussão "SpringBoot 4 support planned?"](https://github.com/MarcGiffing/bucket4j-spring-boot-starter/discussions/433), [`com.bucket4j:bucket4j_jdk17-lettuce` no Maven Central](https://central.sonatype.com/artifact/com.bucket4j/bucket4j_jdk17-lettuce).

---

## Migrations — numeração

A última migration do `java-soulie` é a **V18**. Todas as novas entram a partir da V19, em `src/main/resources/db.migrations/` (pasta **com ponto**), sintaxe só Postgres:

| Versão | Conteúdo | Fase |
|---|---|---|
| `V19__soulup_legacy_alignment.sql` | o que as V12/V13 do `soulie-soul-up` faziam, renumerado: `missions.status` (o `avatars.experience` já veio na V12 do `java-soulie` como `xp` — conferir e não duplicar) | 2 |
| `V20__validation_result_normalization.sql` | `validations.result_validation`: `APROVED` → `APPROVED`, `REJECT` → `REJECTED`; default `PENDING` | 2 |
| `V21__evidence_ai_pipeline.sql` | `evidences`: `daily_mission_id`, `image_url`, `image_key`, `content_type`, `size_bytes`, `submitted_at`, `mission_id` passa a NULL, `CHECK` de exclusividade. `validations`: `detections`, `confidence`, `reason`, `provider`, `requested_at`, `resolved_at`, `attempts` | 4 |
| `V22__social_follows.sql` | tabela `follows` com `UNIQUE(follower_id, followee_id)` e `CHECK(follower_id <> followee_id)` + índices nos dois sentidos | 5 |

Depois disso, regenerar (ou remover) `src/main/resources/db/soulie-schema.sql`, que hoje está parado na V12 e é uma segunda fonte de verdade.

---

## Fases e portões

Cada fase termina com o projeto **compilando** e o contrato `/v1` **funcionando** — o app do parceiro não pode ficar quebrado no meio do caminho. O portão de cada fase é: `./mvnw clean verify` passa + `security-auditor` revisa o diff.

| Fase | O que entra | Depende de | Paralelizável? |
|---|---|---|---|
| **0** | `CLAUDE.md`, `.claude/agents/`, `.claude/skills/`, hook de segurança, `.gitignore` + `.env.example`. Branch nova a partir de `feature/adding-soulie-app-endpoints`. | — | — |
| **1** | Correções de segurança 1–6, 8, 9 da Parte 7. Perfis do Spring (`dev`/`prod`/`test`). Primeiro: baseline — confirmar que o projeto compila **antes** de mexer. | 0 | — |
| **2** | Port do `soulie-soul-up`: domínios, validators, `BusinessException`, os 5 controllers sem colisão. `@Builder` nas entidades. Migrations V19/V20. | 1 | — |
| **3** | Port dos 50 testes + `application-test.yaml` H2 + `spring-boot-starter-test` e `h2` no pom. **É o portão mais importante**: a partir daqui existe rede de segurança de verdade. | 2 | — |
| **4** | Pipeline de IA: storage S3, RabbitMQ, `ImageValidationClient`, webhook HMAC, migration V21, refatorar `MissionApiService.validate`. | 3 | — |
| **5** | Social: `domain/social/`, endpoints, migration V22, integração com nudge/ranking/feed. | 3 | **sim** — independente da Fase 4 (arquivos e tabelas distintos) |
| **6** | Cloudflare (`ClientIpResolver`, `ForwardedHeaderFilter`) + rate limit (Bucket4j/Redis). Correções 10 da Parte 7. | 3 | parcialmente (depende do 4/5 só para aplicar limite nas rotas novas) |
| **7** | Documentação `docs/01`–`08` + README + `SpringDocConfig` atualizado. Revisão final de segurança. | 4, 5, 6 | — |

As fases **4 e 5 são genuinamente independentes** e podem rodar em sessões separadas. As fases 0–3 são sequenciais: cada uma é pré-requisito da seguinte.

---

## Verificação

**Baseline (antes de tudo, Fase 1):** `./mvnw -q clean compile` para confirmar que a branch compila hoje. Sem isso não há como saber se uma quebra é nossa.

**Por fase:**
- `./mvnw clean verify` — a partir da Fase 3 isso roda os 50 testes portados em H2, sem precisar de Postgres nem Docker.
- `security-auditor` sobre o diff da fase.

**Fluxo de validação por IA (Fase 4), ponta a ponta:**
1. subir Postgres, Redis e RabbitMQ (no Railway ou local);
2. `POST /login` → token;
3. `POST /v1/missions/{id}/validate` com uma imagem real → esperar **202** e um `validationId`;
4. conferir no banco: linha em `evidences` com `image_key` preenchido e `validations.result = PENDING`;
5. conferir no bucket que o objeto existe;
6. com a IA real ou um stub que devolva `APPROVED`: confirmar que o worker consome, grava `APPROVED`, credita XP/pontos/impacto, cria `UserActivity`, sincroniza conquistas e dispara o push;
7. `GET /v1/missions/validations/{id}` → resposta completa;
8. caminho triste: fazer a IA devolver erro e confirmar retry → DLQ, sem XP creditado;
9. webhook: `POST /webhooks/validations` com assinatura **errada** → 401; com a certa → processa; **repetido** → idempotente, não credita duas vezes.

**Rate limit (Fase 6):** 6 tentativas de `POST /login` seguidas → a 6ª responde 429 com `Retry-After`. Com duas instâncias apontando para o mesmo Redis, o limite tem de ser compartilhado. E o teste que importa de verdade: mandar `CF-Connecting-IP` falso de fora de faixa Cloudflare **não** deve resetar o contador.

**Social (Fase 5):** A segue B → `GET /friends` vazio dos dois lados; B segue A → ambos aparecem como amigos; A agora consegue dar nudge em B mesmo sem jardim compartilhado; A deixa de seguir → amizade desfeita; self-follow rejeitado.

**Regressão do contrato do app (todas as fases):** comparar o `GET /v3/api-docs` antes e depois. Nenhum campo pode desaparecer das respostas `/v1` — só podem aparecer campos novos.

---

## Fora de escopo (decisões conscientes)

- **Testcontainers** — sem Docker nesta máquina. As migrations não ficam cobertas por teste; recomendação registrada para o CI.
- **Refresh token / revogação de JWT** — a expiração de 2h continua; corrigimos só o fuso fixo. Mudar isso mexe no login do app.
- **Construir a IA** — não é nossa responsabilidade (decisão sua). Entregamos o contrato documentado e o cliente pronto.
- **Cloudflare R2** — as imagens vão para o bucket do Railway.
- **Unificar `Mission` e `DailyMission`** em um só modelo. Seria o ideal arquitetural, mas quebraria o contrato `/v1` do app. Ficam coexistindo, em tabelas separadas, com a razão documentada.

