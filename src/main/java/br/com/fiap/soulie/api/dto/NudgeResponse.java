package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NudgeResponse(
        @Schema(example = "2", description = "Lembretes que ainda podem ser enviados hoje") int remaining
) {
}
