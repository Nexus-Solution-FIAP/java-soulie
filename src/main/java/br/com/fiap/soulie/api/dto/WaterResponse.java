package br.com.fiap.soulie.api.dto;

public record WaterResponse(
        boolean watered,
        String message,
        int streakDays
) {
}
