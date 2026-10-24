package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Contexto opcional da conversa")
public record ChatContext(
        @Valid
        @Schema(description = "Missão aberta na tela, se houver") ChatMissionContext mission,
        @Min(value = 0, message = "Nível do Soulie inválido")
        @Max(value = 3, message = "Nível do Soulie inválido")
        @Schema(example = "1", description = "Nível atual do Soulie: 0 semente, 1 broto, 2 planta jovem, 3 árvore") Integer userLevel
) {
}
