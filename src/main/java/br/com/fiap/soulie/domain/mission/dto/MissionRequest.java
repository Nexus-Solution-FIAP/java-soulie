package br.com.fiap.soulie.domain.mission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MissionRequest(

        @NotNull
        Long actionId,

        @NotBlank
        String description
) {
}
