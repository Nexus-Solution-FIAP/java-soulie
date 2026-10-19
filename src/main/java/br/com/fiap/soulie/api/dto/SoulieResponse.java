package br.com.fiap.soulie.api.dto;

import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Planta do usuário logado")
public record SoulieResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long userId,
        @Schema(example = "Soulie da Sorte") String name,
        @Schema(example = "girassol") FlowerType flowerType,
        @Schema(example = "1", description = "0 semente, 1 broto, 2 planta jovem, 3 árvore") int level,
        @Schema(example = "Broto") String levelName,
        @Schema(example = "85") int xp,
        @Schema(example = "100") int nextLevelXp,
        @Schema(example = "true") boolean wateredToday,
        @Schema(example = "2026-10-19T17:10:00") String lastWateredAt
) {
}
