package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FeedCommentResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "Laura") String author,
        @Schema(example = "L") String avatarLetter,
        @Schema(example = "Boa! Amanhã eu vou junto.") String text,
        @Schema(example = "há 2 min") String time
) {
}
