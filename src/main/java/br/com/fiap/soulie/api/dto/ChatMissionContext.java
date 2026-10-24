package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Missão que o usuário está perguntando para a Soulie IA")
public record ChatMissionContext(
        @Schema(example = "Ir de bike") String name,
        @Schema(example = "Troque o carro pela bike em um trajeto de hoje.") String desc,
        @Schema(example = "40") Integer xp,
        @Schema(example = "Mobilidade") String tag
) {
}
