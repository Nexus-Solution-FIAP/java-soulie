package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil autenticado")
public record UserProfileResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Arthur Silva") String name,
        @Schema(example = "@arthur") String handle,
        @Schema(example = "arthur@email.com") String email,
        @Schema(example = "https://soulie.app/avatar/arthur.png") String avatarUrl,
        @Schema(example = "Membro desde out. 2026") String memberSince,
        UserStatsResponse stats,
        UserSettingsResponse settings
) {
}
