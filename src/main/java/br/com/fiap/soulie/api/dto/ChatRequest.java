package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Pergunta enviada para a Soulie IA")
public record ChatRequest(
        @NotBlank(message = "Mensagem é obrigatória")
        @Size(max = 500, message = "Mensagem muito longa")
        @Schema(example = "Como eu completo a missão de ir de bike?") String message,
        @Valid ChatContext context
) {
}
