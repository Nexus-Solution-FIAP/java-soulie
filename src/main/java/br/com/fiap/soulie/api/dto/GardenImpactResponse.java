package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Impacto somado de quem está no mesmo jardim")
public record GardenImpactResponse(
        @Schema(example = "4.8") double co2Kg,
        @Schema(example = "2.1") double recycledKg,
        @Schema(example = "1.6") double transitCo2Kg,
        @Schema(example = "120") double waterLiters,
        @Schema(example = "9") long actions
) {
}
