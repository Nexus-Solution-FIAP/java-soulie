package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationResponse(
        @Schema(example = "5") Long id,
        @Schema(example = "Arthur lembrou você") String title,
        @Schema(example = "Não deixe sua sequência de 3 dias esfriar.") String body,
        @Schema(example = "2026-10-19T17:30:00") String createdAt
) {
}
