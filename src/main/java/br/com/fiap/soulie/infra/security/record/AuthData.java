package br.com.fiap.soulie.infra.security.record;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login usa o e-mail cadastrado")
public record AuthData(
        @Schema(example = "arthur@email.com") String login,
        @Schema(example = "Soulie@2026") String password
) {
}
