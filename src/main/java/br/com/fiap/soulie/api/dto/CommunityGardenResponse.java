package br.com.fiap.soulie.api.dto;

public record CommunityGardenResponse(
        Long id,
        String name,
        String inviteCode,
        int members
) {
}
