package br.com.fiap.soulie.api.dto;

public record AchievementResponse(
        Long id,
        String name,
        String desc,
        String icon,
        String color,
        String category,
        boolean unlocked,
        String date,
        int xp,
        String progress
) {
}
