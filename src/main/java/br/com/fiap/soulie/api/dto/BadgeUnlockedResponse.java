package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BadgeUnlockedResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "Chama Constante") String name,
        @Schema(example = "Mantenha uma sequência de 7 dias.") String desc,
        @Schema(example = "flame") String icon
) {
}
