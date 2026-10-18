package br.com.fiap.soulie.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RankingItemResponse(
        int pos,
        Long userId,
        String name,
        String avatar,
        int xp,
        int streak,
        @JsonProperty("isYou") boolean isYou,
        int level,
        String flowerType
) {
}
