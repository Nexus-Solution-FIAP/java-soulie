package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Ids das notificações que devem sair da lista")
public record ReadNotificationsRequest(
        @Schema(example = "[1, 2]") List<Long> ids
) {
}
