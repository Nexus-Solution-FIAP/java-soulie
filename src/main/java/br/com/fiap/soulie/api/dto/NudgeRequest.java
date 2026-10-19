package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lembrete para alguém do mesmo jardim não perder a sequência")
public record NudgeRequest(
        @Schema(example = "2", description = "Id do usuário avisado") Long userId
) {
}
