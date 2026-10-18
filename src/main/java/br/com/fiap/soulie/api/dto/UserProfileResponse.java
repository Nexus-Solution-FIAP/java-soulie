package br.com.fiap.soulie.api.dto;

public record UserProfileResponse(
        Long id,
        String name,
        String handle,
        String email,
        String avatarUrl,
        String memberSince,
        UserStatsResponse stats,
        UserSettingsResponse settings
) {
}
