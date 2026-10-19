package br.com.fiap.soulie.infra.security.record;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenDataJWT(
        @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.exemplo.assinatura", description = "Envie como Authorization: Bearer {tokenJWT}") String tokenJWT
) {
}
