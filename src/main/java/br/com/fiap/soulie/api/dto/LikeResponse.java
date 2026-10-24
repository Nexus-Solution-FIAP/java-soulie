package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LikeResponse(
        @Schema(example = "true") boolean liked,
        @Schema(example = "3") long likes
) {
}
