package br.com.fiap.soulie.api.dto;

public record UserSettingsResponse(
        boolean notificationsEnabled,
        boolean emailWeeklyDigest
) {
}
