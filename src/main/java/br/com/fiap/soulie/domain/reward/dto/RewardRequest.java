package br.com.fiap.soulie.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Pedido de resgate.
 *
 * <p>Carrega **apenas** o id do item do catálogo. A versão anterior recebia
 * {@code benefits} (texto livre) e {@code pointsSpent} (o preço) do cliente, e o
 * validator comparava o saldo com o número que o próprio cliente havia mandado —
 * ou seja, dava para resgatar qualquer coisa por 1 ponto. O custo e o texto agora
 * vêm do servidor, do {@code reward_catalog}.
 */
public record RewardRequest(
        @NotNull(message = "Informe qual recompensa quer resgatar")
        @Schema(example = "1", description = "Id do item em GET /reward/catalog")
        Long rewardCatalogId
) {
}
