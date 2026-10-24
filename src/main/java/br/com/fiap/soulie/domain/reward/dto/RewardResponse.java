package br.com.fiap.soulie.domain.reward.dto;

import br.com.fiap.soulie.domain.reward.Reward;
import io.swagger.v3.oas.annotations.media.Schema;

public record RewardResponse(
        @Schema(example = "7") Long id,

        @Schema(example = "Semente especial para o seu Soulie") String benefits,

        @Schema(example = "100") int pointsSpent,

        @Schema(example = "20", description = "Saldo de pontos do usuário depois do resgate") int remainingPoints
) {

    /**
     * O saldo vem por parâmetro, não de {@code reward.getUser().getPoints()}.
     * O débito é feito por uma consulta de atualização em massa, que não
     * atualiza a entidade já carregada — ler dali devolveria o saldo anterior.
     */
    public static RewardResponse of(Reward reward, int remainingPoints) {
        return new RewardResponse(
                reward.getId(),
                reward.getBenefits(),
                reward.getPointsSpent(),
                remainingPoints
        );
    }
}
