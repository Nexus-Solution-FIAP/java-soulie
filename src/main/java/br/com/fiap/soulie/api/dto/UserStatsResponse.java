package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Números do perfil. A sequência conta o dia de hoje ou de ontem; um dia pulado zera até a próxima ação.")
public record UserStatsResponse(
        @Schema(example = "3") int streakDays,
        @Schema(example = "85") int totalXp,
        @Schema(example = "14") int globalRank,
        @Schema(example = "2", description = "Posição entre quem está no mesmo jardim") int friendsRank,
        @Schema(example = "30") int xpToNextRank,
        @Schema(example = "2") int unlockedAchievementsCount,
        @Schema(example = "1.2") double co2SavedKg,
        @Schema(example = "40") double waterSavedLiters,
        @Schema(example = "3") int plantsUnlocked,
        @Schema(example = "16") int plantsTotal
) {
}
