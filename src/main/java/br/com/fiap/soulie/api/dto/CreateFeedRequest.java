package br.com.fiap.soulie.api.dto;

public record CreateFeedRequest(
        String text,
        String imageUri,
        String missionName,
        Integer xp
) {
}
