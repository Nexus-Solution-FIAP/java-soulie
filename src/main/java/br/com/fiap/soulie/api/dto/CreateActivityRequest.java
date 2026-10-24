package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = """
        Atividade ecológica livre registrada pelo app, para o histórico.

        NÃO conclui missão: desde que a validação por IA existe, missão do catálogo \
        só é concluída por POST /v1/missions/{id}/validate, com foto. Se o texto \
        casar com o nome de uma missão, a requisição é recusada com orientação.""")
public record CreateActivityRequest(
        @Schema(example = "🚲", description = "Emoji mostrado no histórico")
        String emoji,

        @Schema(example = "Caminhei até o trabalho", description = "Descrição da ação")
        String label,

        /**
         * XP da atividade livre.
         *
         * <p>O teto não é formalidade. Este valor ia direto para
         * {@code user.addPoints()} e {@code avatar.addXp()} sem limite nenhum:
         * {@code {"xp": 2000000000}} estourava o {@code int} numa requisição,
         * virando o saldo negativo e corrompendo o ranking. 50 é o XP da missão
         * diária mais valiosa — atividade livre não deve valer mais que isso.
         */
        @Min(value = 0, message = "O XP não pode ser negativo")
        @Max(value = 50, message = "Uma atividade livre vale no máximo 50 XP")
        @Schema(example = "10", description = "XP da atividade livre. Máximo 50.")
        Integer xp
) {
}
