package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record WaterResponse(
        @Schema(example = "true", description = "A chamada foi aceita. watered true também quando a planta já tinha sido regada hoje.") boolean watered,
        @Schema(example = "💧 Seu Soulie foi regado! Sequência: 3 dias.") String message,
        @Schema(example = "3") int streakDays
) {
}
