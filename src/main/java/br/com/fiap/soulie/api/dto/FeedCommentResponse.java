package br.com.fiap.soulie.api.dto;

public record FeedCommentResponse(
        Long id,
        String author,
        String avatarLetter,
        String text,
        String time
) {
}
