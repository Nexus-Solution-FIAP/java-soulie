package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SuggestedMissionResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "Separar o reciclável") String name,
        @Schema(example = "Reciclagem") String tag,
        @Schema(example = "30") int xp
) {
}
