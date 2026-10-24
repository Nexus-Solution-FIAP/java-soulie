package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pergunta enviada para a Soulie IA")
public record ChatRequest(
        @Schema(example = "Como eu completo a missão de ir de bike?") String message,
        ChatContext context
) {
}
