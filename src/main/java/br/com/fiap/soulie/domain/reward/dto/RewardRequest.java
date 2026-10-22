package br.com.fiap.soulie.domain.reward.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RewardRequest(

        @NotBlank
        String benefits,

        @Positive
        int pointsSpent
) {
}
