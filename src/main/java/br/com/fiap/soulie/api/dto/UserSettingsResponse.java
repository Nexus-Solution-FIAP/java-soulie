package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserSettingsResponse(
        @Schema(example = "true") boolean notificationsEnabled,
        @Schema(example = "false") boolean emailWeeklyDigest,
        @Schema(example = "true") boolean allowNudges,
        @Schema(example = "true") boolean showFullName
) {
}
