package br.com.fiap.soulie.api.dto;

public record CreateActivityRequest(
        String emoji,
        String label,
        Integer xp
) {
}
