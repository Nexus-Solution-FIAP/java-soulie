package br.com.fiap.soulie.api.dto;

public record UpdateSettingsRequest(
        Boolean notificationsEnabled,
        Boolean emailWeeklyDigest
) {
}
