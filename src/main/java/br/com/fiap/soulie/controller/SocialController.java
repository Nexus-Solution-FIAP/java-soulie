package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.social.FollowService;
import br.com.fiap.soulie.domain.social.dto.FollowResponse;
import br.com.fiap.soulie.domain.social.dto.SocialUserResponse;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


/**
 * Seguir pessoas e ver quem sao seus amigos.
 *
 * <p>Seguir e unilateral e imediato. Quando os dois se seguem, viram amigos —
 * e e a amizade mutua que libera cutucada mesmo sem jardim em comum.
 */
@RestController
@RequestMapping({"/api/v1/social", "/v1/social"})
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Social", description = "Seguir pessoas, amizade mutua e busca por @")
public class SocialController {

    @Autowired
    private FollowService followService;

    @PostMapping("/follow/{userId}")
    @Operation(summary = "Segue uma pessoa",
            description = "Seguir e imediato, sem pedido de aceite. Se ela ja te segue, voces viram amigos e `friendsNow` vem true. Seguir de novo nao da erro.")
    public ResponseEntity<FollowResponse> seguir(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "Id de quem voce quer seguir", example = "7") @PathVariable Long userId) {
        return ResponseEntity.ok(followService.seguir(user, userId));
    }

    @DeleteMapping("/follow/{userId}")
    @Operation(summary = "Deixa de seguir",
            description = "Desfaz a amizade por consequencia: sem os dois sentidos, nao ha amizade.")
    public ResponseEntity<FollowResponse> deixarDeSeguir(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "Id de quem voce quer deixar de seguir", example = "7") @PathVariable Long userId) {
        return ResponseEntity.ok(followService.deixarDeSeguir(user, userId));
    }

    @GetMapping("/followers")
    @Operation(summary = "Quem me segue")
    public ResponseEntity<Page<SocialUserResponse>> seguidores(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(followService.seguidores(user, pageable));
    }

    @GetMapping("/following")
    @Operation(summary = "Quem eu sigo")
    public ResponseEntity<Page<SocialUserResponse>> seguindo(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(followService.seguindo(user, pageable));
    }

    @GetMapping("/friends")
    @Operation(summary = "Meus amigos",
            description = "Quem voce segue e te segue de volta. Sao essas pessoas que voce pode cutucar mesmo sem dividir o jardim.")
    public ResponseEntity<Page<SocialUserResponse>> amigos(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            // Paginado como as outras listas. Sem teto, a rota hidratava o User
            // inteiro de cada amigo (hash de senha incluso) na memoria do
            // processo, e um atacante inflava a propria lista para encarecer
            // cada requisicao.
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(followService.amigos(user, pageable));
    }

    @GetMapping("/users")
    @Operation(summary = "Acha alguem pelo @",
            description = "Busca por handle EXATO. Nao ha busca por nome: ela permitiria varrer a base inteira e montar a lista de quem tem conta.")
    public ResponseEntity<SocialUserResponse> buscar(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "O @ da pessoa, com ou sem arroba", example = "@ana") @RequestParam String handle) {
        return ResponseEntity.ok(followService.buscarPorHandle(user, handle));
    }
}
