package br.com.fiap.soulie.api.dto;

public record GardenImpactResponse(
        double co2Kg,
        double recycledKg,
        double transitCo2Kg,
        double waterLiters,
        long actions
) {
}
