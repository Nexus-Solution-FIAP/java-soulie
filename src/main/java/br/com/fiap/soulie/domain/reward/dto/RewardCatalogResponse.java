package br.com.fiap.soulie.domain.reward.dto;

import br.com.fiap.soulie.domain.reward.RewardCatalog;
import io.swagger.v3.oas.annotations.media.Schema;

public record RewardCatalogResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "Semente especial para o seu Soulie") String benefits,

        @Schema(example = "100", description = "Custo em pontos, definido pelo servidor") int cost
) {

    public RewardCatalogResponse(RewardCatalog catalog) {
        this(catalog.getId(), catalog.getBenefits(), catalog.getCost());
    }
}
