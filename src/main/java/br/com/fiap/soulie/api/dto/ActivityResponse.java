package br.com.fiap.soulie.api.dto;

public record ActivityResponse(
        Long id,
        String emoji,
        String label,
        int xp,
        String date,
        String timestamp
) {
}
