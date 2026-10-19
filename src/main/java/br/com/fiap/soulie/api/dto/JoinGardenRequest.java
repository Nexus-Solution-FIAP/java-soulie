package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Convite de 6 caracteres gerado ao abrir um jardim")
public record JoinGardenRequest(
        @Schema(example = "AB12CD") String code
) {
}
