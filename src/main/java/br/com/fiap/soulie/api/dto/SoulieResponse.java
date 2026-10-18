package br.com.fiap.soulie.api.dto;

import br.com.fiap.soulie.domain.avatar.enums.FlowerType;

public record SoulieResponse(
        Long id,
        Long userId,
        String name,
        FlowerType flowerType,
        int level,
        String levelName,
        int xp,
        int nextLevelXp,
        boolean wateredToday,
        String lastWateredAt
) {
}
