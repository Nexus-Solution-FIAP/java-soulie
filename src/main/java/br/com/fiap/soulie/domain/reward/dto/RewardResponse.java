package br.com.fiap.soulie.domain.reward.dto;

import br.com.fiap.soulie.domain.reward.Reward;

public record RewardResponse(

        Long id,

        String benefits,

        int pointsSpent,

        int remainingPoints
) {
    public RewardResponse(Reward reward) {
        this(
                reward.getId(),
                reward.getBenefits(),
                reward.getPointsSpent(),
                reward.getUser().getPoints()
        );
    }
}
