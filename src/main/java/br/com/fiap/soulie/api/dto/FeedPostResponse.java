package br.com.fiap.soulie.api.dto;

public record FeedPostResponse(
        Long id,
        String author,
        String avatarLetter,
        String avatarColor,
        String text,
        String time,
        long likes,
        int comments,
        boolean liked,
        String imageUri,
        String missionName,
        Integer xp
) {
}
