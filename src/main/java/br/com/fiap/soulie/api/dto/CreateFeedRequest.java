package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Publicação no feed do jardim")
public record CreateFeedRequest(
        @Schema(example = "Fui de bike até a FIAP e evitei o trânsito.") String text,
        @Schema(example = "https://cdn.example/foto.jpg", description = "URL HTTP da foto, opcional. blob: e file: são ignorados.") String imageUri,
        @Schema(example = "Ir de bike") String missionName,
        @Schema(example = "40") Integer xp,
        @Schema(example = "42", description = "Validação aprovada cuja evidência vira a foto do post") Long validationId
) {
}
