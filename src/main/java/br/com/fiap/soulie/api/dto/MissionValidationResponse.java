package br.com.fiap.soulie.api.dto;

import java.util.List;

public record MissionValidationResponse(
        boolean success,
        int xpGained,
        Long missionId,
        List<String> aiDetections,
        double co2SavedKg,
        BadgeUnlockedResponse badgeUnlocked,
        SuggestedMissionResponse nextSuggestedMission
) {
}
