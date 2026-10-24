---
name: endpoint
description: Adiciona um endpoint REST novo no padrao deste projeto. Use ao criar rota nova para o app - cuida do prefixo duplo, do usuario autenticado, da anotacao do Swagger, do DTO, do rate limit e do teste de seguranca.
---

# Skill: endpoint

## O molde

```java
@RestController
@RequestMapping({"/api/v1/recursos", "/v1/recursos"})
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Recursos", description = "O que esse grupo faz, em português")
public class RecursoController {

    @Autowired
    private RecursoApiService service;

    @GetMapping("/{id}")
    @Operation(summary = "Frase curta", description = "O que o endpoint faz e as regras que o app precisa saber.")
    public ResponseEntity<RecursoResponse> buscar(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "Id do recurso", example = "2") @PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(user, id));
    }
}
```

**O prefixo duplo é obrigatório** — o app usa `/v1`, e `/api/v1` responde o mesmo. Toda rota nova do contrato leva os dois.

## Checklist

1. **DTO** como `record` em `api/dto/`, com `@Schema(example = "...")` nos campos. Request com Bean Validation e mensagem em português.
2. **Usuário logado** sempre via `@Parameter(hidden = true) @AuthenticationPrincipal User user`. Nunca `SecurityContextHolder` em service.
3. **Posse do recurso**: se a rota recebe um id, o service **tem de conferir que o recurso é do usuário logado**. Sem isso é IDOR. Prefira um validator de posse (`MissionOwnerValidator` é o exemplo) a um `if` solto.
4. **Regra de negócio** em `validations/<área>/` como `@Component`, não dentro do controller nem do service.
5. **Erro**: deixe o `ErrorHandler` tratar. `EntityNotFoundException` vira 404, `ValidationException` vira 400. Não monte resposta de erro à mão.
6. **Rate limit**: se a rota é pública, custa dinheiro (IA, upload) ou é passível de spam (seguir, comentar), adicione a faixa em `RateLimitPolicy`.
7. **Rota pública** exige alteração em `SecurityConfig` — e aí ela **precisa** de rate limit.
8. **Teste** estendendo `ControllerTestBase`: o caminho feliz, sem token dá 401, e recurso de outro usuário dá 403 ou 404.

## Status HTTP

- `200` leitura e atualização
- `201` criação, com `Location`
- `202` aceito para processamento assíncrono (é o caso da validação por IA)
- `204` remoção

## Não quebre o contrato

O app mobile do parceiro consome `/v1`. **Campo novo na resposta, pode. Remover ou renomear, não.** Para conferir: compare `GET /v3/api-docs` antes e depois.
