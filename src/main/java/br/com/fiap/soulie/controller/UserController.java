package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserService;
import br.com.fiap.soulie.domain.user.dto.UpdateUserData;
import br.com.fiap.soulie.domain.user.dto.UserRequest;
import br.com.fiap.soulie.domain.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Rotas legadas de usuário.
 *
 * <p>Toda operação sobre uma conta existente age sobre a **própria** conta do
 * usuário autenticado. O `{id}` que sobrou em duas rotas é conferido contra o
 * principal, para não quebrar quem já chama esses caminhos.
 *
 * <p>O app novo usa `/v1/users/me` — veja {@code MeController}.
 */
@RestController
@RequestMapping("/user")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Usuários", description = "Cadastro público e consulta da própria conta. O app novo usa o grupo Perfil.")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da própria conta",
            description = "Só funciona para o seu próprio id. Id de outra pessoa responde 403.")
    public ResponseEntity<UserResponse> userDetails(
            @Parameter(description = "Seu próprio id", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.userDetails(id, user.getId()));
    }

    @GetMapping
    @Operation(summary = "Lista usuários ativos",
            description = "Lista paginada, sem e-mail. O nome respeita a preferência de privacidade de cada pessoa. O tamanho máximo de página é limitado pela configuração.")
    public ResponseEntity<Page<UserResponse>> listUsersDetails(
            @PageableDefault(size = 5, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(service.listUsersDetails(pageable));
    }

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Cadastra uma conta", description = "Não precisa de token. Depois chame POST /login. A senha de exemplo é Soulie@2026.")
    public ResponseEntity<UserResponse> registerUser(@RequestBody @Valid UserRequest userRequest,
                                                     UriComponentsBuilder uriBuilder) {
        var detail = service.registerUser(userRequest);
        var uri = uriBuilder.path("/user/{id}").buildAndExpand(detail.id()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }

    @PutMapping
    @Operation(summary = "Atualiza a própria conta",
            description = "Nome, e-mail e senha. A senha é sempre criptografada antes de ser gravada.")
    public ResponseEntity<UserResponse> updateUser(@RequestBody @Valid UpdateUserData updateUserData,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.updateUser(updateUserData, user.getId()));
    }

    /**
     * Reativa a própria conta.
     *
     * <p>Atenção: na prática esta rota é **inalcançável**. Uma conta com
     * {@code active = false} não autentica mais (o {@code SecurityFilter} recusa
     * o token e o login lança {@code DisabledException}), então ninguém consegue
     * chegar aqui para reativá-la. Ela deixou de ser um buraco de segurança —
     * antes qualquer autenticado reativava a conta de qualquer outro — mas
     * continua sem servir para nada.
     *
     * <p>Reativação de verdade exige rota **pública com prova de posse do
     * e-mail** (token enviado por e-mail), o que está fora do escopo atual e
     * está registrado como pendência em {@code docs/07-seguranca.md}.
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Reativa a própria conta",
            description = "Só aceita o seu próprio id. Veja a pendência conhecida em docs/07-seguranca.md: uma conta desativada não consegue autenticar, logo não consegue chamar esta rota.")
    public ResponseEntity<UserResponse> activateUser(
            @Parameter(description = "Seu próprio id", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.activeUser(id, user.getId()));
    }

    @DeleteMapping
    @Operation(summary = "Desativa a própria conta",
            description = "Exclusão lógica da conta do usuário autenticado. O app novo usa DELETE /v1/users/me.")
    public ResponseEntity<Void> excludeUser(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        // Antes: @DeleteMapping sem path mas com @PathVariable Long id. O
        // mapeamento existia, a resolução do argumento lançava
        // MissingPathVariableException, e a rota devolvia 500 para qualquer
        // usuário autenticado. Agora age sobre o principal.
        service.excludeUser(user.getId());
        return ResponseEntity.noContent().build();
    }
}
