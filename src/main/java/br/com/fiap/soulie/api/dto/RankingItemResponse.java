package br.com.fiap.soulie.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record RankingItemResponse(
        @Schema(example = "2") int pos,
        @Schema(example = "1") Long userId,
        @Schema(example = "Arthur Silva") String name,
        @Schema(example = "A") String avatar,
        @Schema(example = "85") int xp,
        @Schema(example = "3") int streak,
        @JsonProperty("isYou") @Schema(example = "true") boolean isYou,
        @Schema(example = "1") int level,
        @Schema(example = "girassol") String flowerType
) {
}
