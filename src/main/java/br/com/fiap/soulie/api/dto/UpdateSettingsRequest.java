package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Preferências do usuário. Campos omitidos não são alterados.")
public record UpdateSettingsRequest(
        @Schema(example = "true", description = "Avisos no app") Boolean notificationsEnabled,
        @Schema(example = "false", description = "Resumo semanal por e-mail") Boolean emailWeeklyDigest
) {
}
