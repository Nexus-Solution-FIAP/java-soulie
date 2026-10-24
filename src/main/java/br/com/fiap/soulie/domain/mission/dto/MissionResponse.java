package br.com.fiap.soulie.domain.mission.dto;

import br.com.fiap.soulie.domain.mission.enums.MissionStatus;

public record MissionResponse(

        Long id,

        Long actionId,

        String actionName,

        int points,

        String description,

        MissionStatus status
) {
}
