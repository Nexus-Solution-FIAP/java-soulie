# Soulie API — Guia do Projeto

API REST do **Soulie**, app de bem-estar gamificado com jardim/planta (FIAP, Nexus Solutions).
**Responda sempre em português.** Comentários, mensagens de erro, validação e Swagger também em português.

## Stack

| Item | Valor |
|---|---|
| Spring Boot | **4.1.1** |
| Java | **21** |
| Banco | PostgreSQL (produção), H2 em memória (testes) |
| Migrations | Flyway em `classpath:db.migrations` |
| ORM | Spring Data JPA + MapStruct |
| Auth | JWT próprio (auth0 `java-jwt`), HMAC256, sem OAuth2 Resource Server |
| Docs | springdoc-openapi → `/swagger-ui.html` e `/v3/api-docs` |

### Armadilhas do Boot 4 (não erre por hábito)

- O starter web chama-se **`spring-boot-starter-webmvc`**, não `-web`.
- Os starters de teste são **divididos**: `spring-boot-starter-data-jpa-test`, `-flyway-test`, `-jdbc-test`, `-validation-test`, `-webmvc-test`, `-security-test`.
- As anotações de teste mudaram de pacote: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`, `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`.
- Antes de adicionar qualquer dependência nova, **confirme a coordenada no Maven Central**. Muita biblioteca ainda não tem release para 4.1.

## Estrutura

Pacote base `br.com.fiap.soulie`. **Pacote por feature**, em camadas (estilo FIAP/Alura) — não é hexagonal, não há ports/adapters.

```
api/dto/            records do contrato do app mobile (/v1)
api/TimeLabels      formatação de datas relativas em pt-BR
controller/         @RestController
domain/<agregado>/  Entity + Repository + Service + Mapper + dto/
validations/<área>/ cadeia de validators (ver abaixo)
infra/security/     SecurityConfig, SecurityFilter, TokenService, AuthService
infra/exception/    ErrorHandler (@RestControllerAdvice), BusinessException
infra/push/         ExpoPushService
infra/springdoc/    SpringDocConfig
```

### Padrão de 5 peças por domínio

Cada agregado em `domain/<nome>/` tem: a **entidade** JPA (com comportamento de negócio nela, não anêmica), o **repository** Spring Data, o **service** `@Transactional`, o **mapper** MapStruct e os **DTOs** como `record` em `dto/`.

> Nos domínios vindos do `soulie-soul-up` a pasta de DTOs chamava-se `record/`. **A convenção deste projeto é `dto/`.**

### Cadeia de validators

Regras de negócio não ficam em `if` espalhado no service. Cada regra é um `@Component` que implementa uma interface de validação; o service injeta `List<XValidator>` e itera:

```java
@Autowired private List<EvidenceSubmissionValidator> submissionValidators;
...
submissionValidators.forEach(v -> v.validate(mission, userId));
```

Validator lança `jakarta.validation.ValidationException` (vira 400 no `ErrorHandler`) ou `BusinessException`. Para adicionar uma regra, crie um `@Component` novo — **não** mexa no service.

## Convenções de API

- **Prefixo duplo obrigatório** em todo controller do contrato novo:
  `@RequestMapping({"/api/v1/recurso", "/v1/recurso"})`
- Rotas legadas sem prefixo: `/login`, `/user`, `/avatar`, `/garden`, `/action`, `/mission`, `/evidence`, `/reward`, `/history-points`.
- **Usuário logado sempre assim**, com o parâmetro oculto no Swagger:
  ```java
  public ResponseEntity<X> metodo(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
  ```
  Nunca `SecurityContextHolder` dentro de service.
- Todo endpoint leva `@Operation(summary = "...", description = "...")` em português e a classe leva `@Tag`.
- DTO de resposta do contrato novo: `record` em `api/dto/`, com `@Schema(example = "...")` nos campos.

### Regra dura: não quebre o contrato `/v1`

O app mobile do parceiro de equipe consome `/v1`. **Nenhum campo pode ser removido ou renomeado** numa resposta `/v1` sem avisar explicitamente. Campos novos, sim. Em dúvida, compare `GET /v3/api-docs` antes e depois.

## Migrations

- Pasta: `src/main/resources/db.migrations/` — **com ponto, não barra**. Está assim por causa de `spring.flyway.locations: classpath:db.migrations` no `application.yaml`.
- Nome: `V<n>__snake_case_descricao.sql`, `n` sem zero à esquerda.
- **Sempre confira o maior número existente antes de criar** (`ls src/main/resources/db.migrations/`). Migration já aplicada nunca é editada.
- Sintaxe **só PostgreSQL**. O perfil de teste não roda Flyway (gera o schema das entidades), então sintaxe exótica não quebra teste — quebra produção.
- **O schema é o que as migrations produzem, e nada mais.** Não há dump para manter em paralelo: o antigo `db/soulie-schema.sql` foi removido por estar 8 migrations atrasado prometendo ser o "schema final". Não recrie esse padrão.
- **Sempre declare `@Column(name = ..., length = ...)` explicitamente na entidade.** Não confie na nomeação implícita nem no tamanho padrão. O schema dos testes vem das **entidades** e o de produção vem das **migrations**: se a entidade for implícita, os dois divergem e o teste passa a validar um schema que não é o de produção. Dois casos reais já aconteceram aqui:
  - `resultValidation` sem `@Column`: em produção o Hibernate converte para `result_validation` e acerta, mas no perfil de teste o `globally_quoted_identifiers` **pula** essa conversão e gera `resultValidation`. O teste nunca exercitava o nome real da coluna.
  - campo `String` sem `length`: o H2 gera `varchar(255)` enquanto a migration tem `varchar(100)`. Um nome de 200 caracteres **passava no teste e falhava em produção**.

## Testes

- Ficam em `src/test/java/br/com/fiap/soulie/`, perfil `test` (`src/test/resources/application-test.yaml`).
- H2 em memória com `MODE=PostgreSQL`, `flyway.enabled=false`, `ddl-auto=create-drop` e `globally_quoted_identifiers=true` — este último é **necessário** porque `level` e `date` são palavras reservadas no H2.
- **Não há Docker nesta máquina**, então não use Testcontainers.
- Classes-base: `RepositoryTestBase` (`@DataJpaTest` + helpers que persistem de verdade), `ControllerTestBase` (`MockMvc` + `@MockitoBean` no `TokenService`/`UserRepository` + `@Import(SecurityConfig.class)`), `TestData` (fábrica de entidades).
- As entidades têm `@Builder` para os testes montarem objetos. Ao adicionar campo obrigatório numa entidade, **verifique se os helpers de teste precisam preenchê-lo** (ex.: `Garden.inviteCode` é `nullable=false, unique=true`).
- Rodar: `./mvnw clean verify`.

## Cliente HTTP externo

Há **um** jeito de chamar API de fora, o de `infra/push/ExpoPushService`: `RestClient` + `JdkClientHttpRequestFactory`, timeouts explícitos de connect e read, records aninhados para request/response, falha tratada e logada sem derrubar o fluxo. **Siga esse molde.** Não introduza `RestTemplate`, `WebClient` nem Feign.

## Segurança — regras não negociáveis

- Nenhum segredo em código ou em log. Tudo por variável de ambiente, e `.env` é ignorado pelo Git.
- Todo endpoint que recebe um id precisa **conferir se o recurso pertence ao usuário logado** (IDOR). O padrão do projeto é um validator de posse, como `MissionOwnerValidator`.
- `ErrorHandler` **nunca** devolve mensagem de exceção crua ao cliente.
- Upload de imagem: validar **magic bytes**, não só `Content-Type`. Chave de storage gerada pelo servidor (UUID), nunca o nome do arquivo do cliente.
- Webhook de entrada: HMAC com comparação em **tempo constante** (`MessageDigest.isEqual`), anti-replay por timestamp e idempotência.
- Rate limit atrás do Cloudflare: só confiar em `CF-Connecting-IP` quando a conexão vem de faixa de IP do Cloudflare. Aceitar o header de qualquer origem é bypass direto.
- Ao fim de cada bloco de trabalho, rode o agente `security-auditor`.

## Documentação

`docs/01`–`08` explicam arquitetura, o merge dos dois repositórios, o fluxo de validação por IA, o contrato da IA, Cloudflare/rate limit, social, segurança e operação. Mudou comportamento? Atualize o doc correspondente (a skill `arch-docs` ajuda).

## Histórico importante

Este repositório é a junção de dois forks do mesmo app: `java-soulie` (o contrato `/v1` do app) e `soulie-soul-up` (domínios action/mission/evidence/validation/reward/history_points, a cadeia de validators e a suíte de testes). Por isso convivem **dois modelos de missão**:

- `DailyMission` + `UserMissionCompletion` → tabelas `daily_missions` / `user_mission_completions`, usados pelo contrato `/v1` do app.
- `Mission` (+ `Action`) → tabela `missions`, modelo legado com `MissionStatus` e evidência.

**Não tente unificar** sem decisão explícita: quebraria o contrato `/v1`. O detalhe está em `docs/02-integracao-repositorios.md`.
