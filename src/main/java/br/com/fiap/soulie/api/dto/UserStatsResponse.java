package br.com.fiap.soulie.api.dto;

public record UserStatsResponse(
        int streakDays,
        int totalXp,
        int globalRank,
        int friendsRank,
        int xpToNextRank,
        int unlockedAchievementsCount,
        double co2SavedKg,
        double waterSavedLiters
) {
}
