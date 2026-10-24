package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AchievementResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "Chama Constante") String name,
        @Schema(example = "Mantenha uma sequência de 7 dias.") String desc,
        @Schema(example = "flame") String icon,
        @Schema(example = "#F97316") String color,
        @Schema(example = "Hábito") String category,
        @Schema(example = "false") boolean unlocked,
        @Schema(example = "Em progresso") String date,
        @Schema(example = "60") int xp,
        @Schema(example = "3/7") String progress
) {
}
