package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token Expo Push do aparelho logado")
public record RegisterPushTokenRequest(
        @Schema(example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]") String token
) {
}
