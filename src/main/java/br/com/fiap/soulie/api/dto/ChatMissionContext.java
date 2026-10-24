package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Missão que o usuário está perguntando para a Soulie IA")
public record ChatMissionContext(
        @Size(max = 120, message = "Nome da missão muito longo")
        @Schema(example = "Ir de bike") String name,
        @Size(max = 500, message = "Descrição da missão muito longa")
        @Schema(example = "Troque o carro pela bike em um trajeto de hoje.") String desc,
        @Schema(example = "40") Integer xp,
        @Size(max = 40, message = "Tag da missão muito longa")
        @Schema(example = "Mobilidade") String tag
) {
}
