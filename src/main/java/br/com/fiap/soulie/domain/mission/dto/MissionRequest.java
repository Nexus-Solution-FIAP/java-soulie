package br.com.fiap.soulie.domain.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MissionRequest(
        @NotNull(message = "Informe a ação da missão")
        @Schema(example = "1")
        Long actionId,

        @NotBlank(message = "A descrição da missão é obrigatória")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        @Schema(example = "Separar o lixo de casa durante a semana")
        String description
) {
}
