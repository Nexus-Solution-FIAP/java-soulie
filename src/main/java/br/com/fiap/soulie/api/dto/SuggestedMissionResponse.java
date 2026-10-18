package br.com.fiap.soulie.api.dto;

public record SuggestedMissionResponse(
        Long id,
        String name,
        String tag,
        int xp
) {
}
