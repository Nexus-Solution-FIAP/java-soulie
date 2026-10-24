package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Publicação no feed do jardim")
public record CreateFeedRequest(
        @Schema(example = "Fui de bike até a FIAP e evitei o trânsito.") String text,
        @Schema(example = "file:///data/user/0/soulie/cache/foto.jpg", description = "URI local da foto, opcional") String imageUri,
        @Schema(example = "Ir de bike") String missionName,
        @Schema(example = "40") Integer xp
) {
}
