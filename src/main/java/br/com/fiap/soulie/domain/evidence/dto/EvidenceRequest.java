package br.com.fiap.soulie.domain.evidence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvidenceRequest(

        @NotNull
        Long missionId,

        @NotBlank
        String image

) {
}
