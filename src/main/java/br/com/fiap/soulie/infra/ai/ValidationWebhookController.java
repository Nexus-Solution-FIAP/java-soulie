package br.com.fiap.soulie.infra.ai;

import br.com.fiap.soulie.domain.validation.ValidationOutcomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe o veredito quando a IA do parceiro responde depois, em vez de na
 * própria chamada.
 *
 * <p><b>Este é o endpoint mais sensível do projeto: ele concede XP e é
 * público.</b> Três proteções, e nenhuma delas é opcional:
 *
 * <ol>
 *   <li><b>Assinatura HMAC</b> sobre {@code timestamp + "." + corpo}, comparada
 *       em tempo constante. Sem ela, quem descobrisse a URL aprovaria a própria
 *       evidência.</li>
 *   <li><b>Janela de timestamp</b>, porque uma requisição legítima capturada e
 *       reenviada depois creditaria XP de novo.</li>
 *   <li><b>Idempotência</b>, garantida lá embaixo pelo {@code UPDATE}
 *       condicional do {@code ValidationService}: a mesma chamada repetida não
 *       credita duas vezes.</li>
 * </ol>
 *
 * <p>Fica <b>desligado por padrão</b> ({@code soulie.ai.webhook-enabled}). Só
 * deve subir se a IA do parceiro for do tipo que responde depois — um endpoint
 * público a mais que ninguém usa é só superfície de ataque.
 */
@RestController
@RequestMapping("/webhooks/validations")
@SecurityRequirements
@ConditionalOnProperty(name = "soulie.ai.webhook-enabled", havingValue = "true")
@Tag(name = "Webhook de validação", description = "Callback assinado da IA de validação de imagem")
public class ValidationWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ValidationWebhookController.class);

    private static final String HEADER_ASSINATURA = "X-Soulie-Signature";
    private static final String HEADER_TIMESTAMP = "X-Soulie-Timestamp";

    @Autowired
    private AiValidationProperties properties;

    @Autowired
    private HmacSignature hmacSignature;

    @Autowired
    private ValidationOutcomeService outcomeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * O corpo chega como {@code String} crua de propósito: a assinatura é
     * calculada sobre os bytes exatos recebidos. Deixar o Spring desserializar e
     * depois re-serializar para conferir mudaria espaçamento e ordem de campos,
     * e a assinatura nunca bateria.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Resultado da validação",
            description = "Exige os cabeçalhos X-Soulie-Signature (HMAC-SHA256 hex de timestamp + '.' + corpo) e X-Soulie-Timestamp (epoch em segundos).")
    public ResponseEntity<Void> receber(
            @RequestBody String corpo,
            @RequestHeader(value = HEADER_ASSINATURA, required = false) String assinatura,
            @RequestHeader(value = HEADER_TIMESTAMP, required = false) String timestamp) {

        String segredo = properties.getWebhookSecret();
        if (segredo == null || segredo.isBlank()) {
            log.error("Webhook habilitado sem soulie.ai.webhook-secret configurado");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        if (!hmacSignature.timestampValido(timestamp, properties.getWebhookTolerance())) {
            log.warn("Webhook recusado: timestamp ausente ou fora da janela");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!hmacSignature.confere(segredo, timestamp, corpo, assinatura)) {
            log.warn("Webhook recusado: assinatura inválida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ImageValidationContract.WebhookPayload payload;
        try {
            payload = objectMapper.readValue(corpo, ImageValidationContract.WebhookPayload.class);
        } catch (Exception exception) {
            log.warn("Webhook com corpo inválido");
            return ResponseEntity.badRequest().build();
        }

        if (payload.validationId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Idempotente: a repetição é barrada pelo UPDATE condicional lá dentro.
        outcomeService.aplicar(payload.validationId(), payload.toResult());
        return ResponseEntity.noContent().build();
    }
}
