package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityGardenResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Jardim do Arthur") String name,
        @Schema(example = "AB12CD") String inviteCode,
        @Schema(example = "3") int members
) {
}
