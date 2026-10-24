package br.com.fiap.soulie.domain.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de seguir alguem.
 *
 * @param following  voce passou a seguir essa pessoa
 * @param friendsNow a outra pessoa ja te seguia, entao voces viraram amigos
 *                   agora — e isso libera cutucada mesmo sem jardim em comum
 */
public record FollowResponse(
        @Schema(example = "true") boolean following,
        @Schema(example = "false", description = "Virou amizade mutua nesta acao") boolean friendsNow,
        SocialUserResponse user
) {
}
