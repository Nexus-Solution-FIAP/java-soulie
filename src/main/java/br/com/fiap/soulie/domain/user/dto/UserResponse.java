package br.com.fiap.soulie.domain.user.dto;

import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta das rotas legadas de usuário.
 *
 * <p>O campo {@code email} foi removido. Antes, `GET /user` era uma lista
 * paginada sem teto efetivo de tamanho, aberta a qualquer usuário autenticado —
 * o que permitia colher o e-mail de toda a base com uma requisição. E o
 * {@code name} passou a vir de {@link User#publicName()}, respeitando o
 * {@code showFullName} que o resto do projeto já honrava e esta resposta
 * ignorava.
 *
 * <p>Este DTO não é usado por nenhuma rota {@code /v1} — o contrato do app tem
 * o seu próprio {@code api/dto/UserProfileResponse}.
 */
public record UserResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "Ana") String name,

        @Schema(example = "120") int points,

        @Schema(example = "true") Boolean active
) {

    public UserResponse(User user) {
        this(
                user.getId(),
                user.publicName(),
                user.getPoints(),
                user.getActive()
        );
    }
}
