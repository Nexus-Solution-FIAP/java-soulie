package br.com.fiap.soulie.domain.evidence.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Envio de evidência de uma missão.
 *
 * <p>O {@code @Pattern} em {@code image} não é formalidade. O campo era apenas
 * {@code @NotBlank}, aceitando qualquer string: {@code file:///etc/passwd},
 * {@code http://169.254.169.254/latest/meta-data/} (o endpoint de metadados da
 * infraestrutura), {@code http://localhost:8080/actuator/env} ou
 * {@code javascript:...}. Hoje o valor só é guardado e devolvido ao próprio
 * dono, mas na Fase 4 o worker vai mandar essa URL para a IA externa e devolver
 * a resposta ao usuário — isto é, viraria <b>SSRF com saída</b> contra a rede
 * interna, e a brecha já estaria plantada agora.
 *
 * <p>Por isso: só {@code https}, host com formato de domínio, e nada de espaço
 * ou sinal de marcação no caminho.
 *
 * <p>Quando a Fase 4 entrar, o cliente deixa de mandar URL: ele envia o arquivo,
 * o servidor gera a chave (UUID) e grava apenas ela. Esta validação é a barreira
 * até lá, e continua valendo depois como defesa em profundidade.
 */
public record EvidenceRequest(
        @NotNull(message = "Informe a missão da evidência")
        @Schema(example = "1")
        Long missionId,

        @NotBlank(message = "A imagem da evidência é obrigatória")
        @Size(max = 500, message = "A URL da imagem deve ter no máximo 500 caracteres")
        @Pattern(
                regexp = "^https://[A-Za-z0-9._-]+(:[0-9]+)?/[^\\s<>\"']*$",
                message = "A imagem deve ser uma URL https válida"
        )
        @Schema(example = "https://storage.exemplo.com/evidencias/8f14e45f.jpg")
        String image
) {
}
