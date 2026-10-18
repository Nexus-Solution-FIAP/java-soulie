package br.com.fiap.soulie.api.dto;

public record LikeResponse(
        boolean liked,
        long likes
) {
}
