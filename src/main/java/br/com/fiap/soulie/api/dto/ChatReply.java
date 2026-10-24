package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatReply(
        @Schema(example = "Um trajeto curto de bike já vale. Tire uma foto da bike ou do caminho e confirme a missão.") String reply
) {
}
