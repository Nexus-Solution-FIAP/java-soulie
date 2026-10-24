package br.com.fiap.soulie.domain.social.dto;

import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uma pessoa no grafo social.
 *
 * <p>Sem e-mail, e o nome vem de {@code publicName()}, que respeita a
 * preferencia de privacidade do usuario. E a mesma regra que a Fase 1b aplicou
 * ao {@code UserResponse} depois de a auditoria achar que `GET /user` entregava
 * o e-mail de toda a base.
 */
public record SocialUserResponse(
        @Schema(example = "7") Long id,
        @Schema(example = "Ana") String name,
        @Schema(example = "@ana") String handle,
        @Schema(example = "120") int points,
        @Schema(example = "true", description = "Se voce e essa pessoa se seguem") boolean friend
) {

    public static SocialUserResponse of(User user, boolean friend) {
        return new SocialUserResponse(
                user.getId(),
                user.publicName(),
                user.getHandle(),
                user.getPoints(),
                friend);
    }
}
