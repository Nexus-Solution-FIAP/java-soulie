package br.com.fiap.soulie.domain.action.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Criação de ação do catálogo. Operação administrativa.
 *
 * <p>Os limites não são cosméticos. Sem o {@code @Max} em {@code points},
 * um valor como 2.000.000.000 causaria overflow de {@code int} em
 * {@code User.addPoints} e {@code Avatar.addXp} na conclusão da missão, virando
 * o saldo negativo e corrompendo o ranking. Os {@code @Size} espelham os
 * tamanhos das colunas: sem eles, texto longo não era recusado com 400 de
 * validação — ia ao banco e estourava lá, virando um erro com mensagem errada.
 */
public record ActionRequest(
        @NotBlank(message = "O nome da ação é obrigatório")
        @Size(max = 100, message = "O nome da ação deve ter no máximo 100 caracteres")
        @Schema(example = "Separar o lixo reciclável")
        String name,

        @NotBlank(message = "A descrição da ação é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        @Schema(example = "Separe plástico, papel e metal do lixo comum")
        String description,

        @Positive(message = "Os pontos devem ser positivos")
        @Max(value = 1000, message = "Uma ação vale no máximo 1000 pontos")
        @Schema(example = "30")
        int points
) {
}
