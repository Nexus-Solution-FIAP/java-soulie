package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ActivityResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "🚲") String emoji,
        @Schema(example = "Ir de bike realizada") String label,
        @Schema(example = "40") int xp,
        @Schema(example = "Hoje, 17:20") String date,
        @Schema(example = "2026-10-19T17:20:00") String timestamp
) {
}
