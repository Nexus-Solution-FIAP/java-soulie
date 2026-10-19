package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Atividade ecológica registrada pelo app. Se o texto contiver o nome de uma missão do dia ainda não feita, ela é concluída e o XP da missão vale uma vez.")
public record CreateActivityRequest(
        @Schema(example = "🚲", description = "Emoji mostrado no histórico") String emoji,
        @Schema(example = "Ir de bike realizada", description = "Descrição da ação") String label,
        @Schema(example = "40", description = "XP pedido quando a atividade não casa com uma missão") Integer xp
) {
}
