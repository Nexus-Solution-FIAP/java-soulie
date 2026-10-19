package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MissionValidationResponse(
        @Schema(example = "true") boolean success,
        @Schema(example = "40") int xpGained,
        @Schema(example = "2") Long missionId,
        @Schema(example = "[\"Bicicleta reconhecida no trajeto\"]") List<String> aiDetections,
        @Schema(example = "0.4") double co2SavedKg,
        BadgeUnlockedResponse badgeUnlocked,
        SuggestedMissionResponse nextSuggestedMission
) {
}
