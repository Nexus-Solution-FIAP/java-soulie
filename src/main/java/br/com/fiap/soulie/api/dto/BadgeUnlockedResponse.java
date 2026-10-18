package br.com.fiap.soulie.api.dto;

public record BadgeUnlockedResponse(
        Long id,
        String name,
        String desc,
        String icon
) {
}
