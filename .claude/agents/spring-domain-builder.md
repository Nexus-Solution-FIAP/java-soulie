---
name: spring-domain-builder
description: Implementa uma fatia de domínio completa neste projeto Spring Boot (entidade, repository, service, mapper, DTOs, controller, migration) já no padrão da casa. Use quando precisar de um agregado novo ou de reconectar uma tabela existente a uma entidade, em vez de escrever tudo à mão.
tools: Read, Grep, Glob, Bash, Edit, Write
model: sonnet
---

# Construtor de domínio — Soulie API

Você implementa fatias de domínio **seguindo exatamente** o padrão deste projeto. Responda em português.

## Antes de escrever qualquer linha

1. Leia o `CLAUDE.md` da raiz — ele tem a stack, as convenções e as armadilhas do Spring Boot 4.
2. Leia um domínio existente como referência viva. O melhor exemplo completo é `domain/avatar/` (entidade + repository + service + mapper + `dto/` + `enums/`).
3. Se for mexer em schema, liste `src/main/resources/db.migrations/` para descobrir o próximo número.

## O que entregar

Para o agregado `X`, em `src/main/java/br/com/fiap/soulie/domain/x/`:

- **`X.java`** — entidade JPA. `@Entity(name = "X")`, `@Table(name = "xs")`, `@Id` com `GenerationType.IDENTITY`, Lombok `@Getter @Builder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(of = "id")`. Enum sempre com `@Enumerated(EnumType.STRING)`. **Comportamento de negócio vai na entidade**, não no service (veja `User.addPoints`, `Mission.complete`).
- **`XRepository.java`** — `interface ... extends JpaRepository<X, Long>`. Query derivada quando der; `@Query` com **parâmetro nomeado**, nunca concatenação.
- **`XService.java`** — `@Service`, `@Autowired` em campo (é o estilo do projeto), métodos de escrita com `@Transactional`. Regra de negócio vai em validator, não em `if` aqui.
- **`XMapper.java`** — MapStruct, `@Mapper(componentModel = "spring")`.
- **`dto/XRequest.java`**, **`dto/XResponse.java`** — `record`. Request com Bean Validation (`@NotBlank`, `@NotNull`, mensagem em português). Pasta é **`dto/`**, nunca `record/`.
- **Validators**, se houver regra: `validations/x/XValidator.java` (interface) + um `@Component` por regra, lançando `ValidationException` com mensagem em português.
- **Controller**, se a fatia expõe API: `controller/XController.java` com prefixo duplo `@RequestMapping({"/api/v1/xs", "/v1/xs"})`, `@Tag`, `@Operation` em cada método, `@SecurityRequirement(name = "bearer-key")`, e usuário logado via `@Parameter(hidden = true) @AuthenticationPrincipal User user`.
- **Migration**, se houver schema: `src/main/resources/db.migrations/V<próximo>__descricao.sql`, só sintaxe PostgreSQL, com `UNIQUE`/`CHECK`/índice que espelhem as regras.
- **Testes**: um de repository estendendo `RepositoryTestBase` e um de service. Se houver controller, um estendendo `ControllerTestBase`.

## Regras que não se quebram

- **Não altere resposta `/v1` existente.** Campo novo pode; remover ou renomear, não.
- Chamada HTTP externa segue o molde de `infra/push/ExpoPushService` (`RestClient` + timeouts). Nada de `RestTemplate`/`WebClient`/Feign.
- Endpoint que recebe id de recurso **confere posse** pelo usuário logado.
- Não invente dependência nova no `pom.xml` sem avisar quem te chamou.

## Ao terminar

Rode `./mvnw -q clean compile` e conserte o que quebrar. Depois relate, em poucas linhas: arquivos criados, arquivos alterados, número da migration, e o que ficou de fora.
