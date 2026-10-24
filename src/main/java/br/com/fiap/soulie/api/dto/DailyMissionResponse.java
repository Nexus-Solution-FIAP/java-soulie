package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DailyMissionResponse(
        @Schema(example = "2") Long id,
        @Schema(example = "🚲") String emoji,
        @Schema(example = "Ir de bike") String name,
        @Schema(example = "Mobilidade") String tag,
        @Schema(example = "Troque o carro pela bike em um trajeto de hoje.") String desc,
        @Schema(example = "40") int xp,
        @Schema(example = "false") boolean done,
        @Schema(example = "2026-10-19T17:20:00") String completedAt,
        @Schema(example = "daily") String period
) {
}
