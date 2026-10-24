package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contexto opcional da conversa")
public record ChatContext(
        @Schema(description = "Missão aberta na tela, se houver") ChatMissionContext mission,
        @Schema(example = "1", description = "Nível atual do Soulie: 0 semente, 1 broto, 2 planta jovem, 3 árvore") Integer userLevel
) {
}
