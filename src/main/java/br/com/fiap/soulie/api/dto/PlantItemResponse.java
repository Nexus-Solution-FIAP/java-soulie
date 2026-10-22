package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uma planta da coleção do usuário")
public record PlantItemResponse(
        @Schema(example = "pitangueira") String id,
        @Schema(example = "Pitangueira") String name,
        @Schema(example = "true") boolean starterEligible,
        @Schema(example = "true") boolean unlocked,
        @Schema(example = "false") boolean selected,
        @Schema(example = "0") int xp,
        @Schema(example = "0") int level,
        @Schema(example = "Semente") String levelName,
        @Schema(example = "STARTER") String unlockSource,
        @Schema(example = "Leve 1 planta ao nível máximo") String unlockHint
) {
}
