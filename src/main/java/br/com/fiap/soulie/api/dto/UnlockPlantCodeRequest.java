package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Código secreto para liberar uma planta")
public record UnlockPlantCodeRequest(
        @Schema(example = "ROSAFLORA") String code
) {
}
