package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FeedPostResponse(
        @Schema(example = "8") Long id,
        @Schema(example = "Arthur") String author,
        @Schema(example = "A") String avatarLetter,
        @Schema(example = "#0EA5E9") String avatarColor,
        @Schema(example = "Fui de bike até a FIAP.") String text,
        @Schema(example = "agora") String time,
        @Schema(example = "2") long likes,
        @Schema(example = "1") int comments,
        @Schema(example = "false") boolean liked,
        @Schema(example = "https://cdn.example/foto.jpg") String imageUri,
        @Schema(example = "Ir de bike") String missionName,
        @Schema(example = "40") Integer xp,
        @Schema(example = "true", description = "True quando a publicação é do usuário logado") boolean mine
) {
}
