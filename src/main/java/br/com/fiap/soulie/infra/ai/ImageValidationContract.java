package br.com.fiap.soulie.infra.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Contrato com a API de validação de imagem do parceiro.
 *
 * <p>Os dois lados do contrato ficam juntos neste arquivo de propósito: é ele
 * que vai junto de {@code docs/04-contrato-ia.md} quando o contrato for
 * entregue a quem constrói a IA.
 */
public final class ImageValidationContract {

    private ImageValidationContract() {
    }

    /**
     * O que enviamos para a IA.
     *
     * <p>O contexto da missão vai junto porque a pergunta não é "o que há nesta
     * foto?", e sim "esta foto comprova <i>esta</i> ação?". Sem o contexto, a IA
     * não tem como responder o que interessa.
     */
    public record Request(
            @Schema(example = "42", description = "Identificador da validação. Devolva-o no webhook.")
            Long validationId,

            @Schema(example = "https://t3.storageapi.dev/...?X-Amz-Signature=...",
                    description = "URL temporária de leitura. Nula quando a imagem vai em imageBase64.")
            String imageUrl,

            @Schema(description = "Imagem em base64. Preenchida só no modo BYTES.")
            String imageBase64,

            @Schema(example = "image/jpeg") String contentType,

            Mission mission,

            @Schema(description = "Endereço para responder depois, se a análise não for imediata. Nulo quando o webhook está desligado.")
            String callbackUrl
    ) {
    }

    /** Contexto da missão que a foto deveria comprovar. */
    public record Mission(
            @Schema(example = "2") Long id,
            @Schema(example = "Ir de bicicleta") String name,
            @Schema(example = "Mobilidade") String tag,
            @Schema(example = "Troque o carro pela bicicleta em um trajeto do dia") String description,
            @Schema(example = "🚲") String emoji
    ) {
    }

    /**
     * O que a IA devolve — na resposta da própria chamada ou no webhook.
     *
     * <p>Anotada com {@code @JsonIgnoreProperties(ignoreUnknown = true)}: campos
     * extras que o parceiro acrescentar não quebram a desserialização.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @Schema(example = "APPROVED", allowableValues = {"APPROVED", "REJECTED"},
                    description = "Veredito. Qualquer outro valor é tratado como falha de análise.")
            String status,

            @Schema(example = "[\"Bicicleta reconhecida\", \"Trajeto urbano\"]",
                    description = "O que foi reconhecido na imagem. Mostrado ao usuário.")
            List<String> detections,

            @Schema(example = "0.93", description = "Confiança entre 0 e 1.")
            Double confidence,

            @Schema(example = "A foto mostra uma bicicleta em uso na via",
                    description = "Justificativa curta. Mostrada ao usuário quando reprovada.")
            String reason,

            @Schema(example = "vision-v2", description = "Identificação do modelo que decidiu.")
            String provider
    ) {
    }

    /** Corpo que a IA envia ao webhook: o resultado mais o id da validação. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookPayload(
            @Schema(example = "42") Long validationId,
            String status,
            List<String> detections,
            Double confidence,
            String reason,
            String provider
    ) {
        public Result toResult() {
            return new Result(status, detections, confidence, reason, provider);
        }
    }
}
