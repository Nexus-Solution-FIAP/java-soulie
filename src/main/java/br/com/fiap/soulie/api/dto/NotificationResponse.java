package br.com.fiap.soulie.api.dto;

public record NotificationResponse(
        Long id,
        String title,
        String body,
        String createdAt
) {
}
