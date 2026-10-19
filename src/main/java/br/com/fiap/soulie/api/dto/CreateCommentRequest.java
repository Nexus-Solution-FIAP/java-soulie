package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Comentário em uma publicação do feed")
public record CreateCommentRequest(
        @Schema(example = "Boa! Amanhã eu vou junto.") String text
) {
}
