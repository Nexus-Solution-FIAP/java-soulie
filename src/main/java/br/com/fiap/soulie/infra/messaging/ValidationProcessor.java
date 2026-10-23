package br.com.fiap.soulie.infra.messaging;

import br.com.fiap.soulie.domain.validation.ValidationOutcomeService;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.infra.ai.AiValidationProperties;
import br.com.fiap.soulie.infra.ai.ImageValidationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Aplica o veredito de uma validação, fora de requisição e fora de transação.
 *
 * <p>Usado pelo consumidor Rabbit e, no perfil {@code dev}, pelo processamento
 * em processo — sem fila.
 */
@Service
public class ValidationProcessor {

    private static final Logger log = LoggerFactory.getLogger(ValidationProcessor.class);

    private final ValidationService validationService;
    private final ValidationOutcomeService outcomeService;
    private final ImageValidationClient validationClient;
    private final AiValidationProperties aiProperties;
    private final String callbackUrl;

    public ValidationProcessor(ValidationService validationService,
                               ValidationOutcomeService outcomeService,
                               ImageValidationClient validationClient,
                               AiValidationProperties aiProperties,
                               @Value("${soulie.ai.callback-url:}") String callbackUrl) {
        this.validationService = validationService;
        this.outcomeService = outcomeService;
        this.validationClient = validationClient;
        this.aiProperties = aiProperties;
        this.callbackUrl = callbackUrl;
    }

    public void processar(Long validationId) {
        log.debug("Processando validação {}", validationId);

        var dados = validationService.carregarParaAnalise(validationId).orElse(null);

        if (dados == null) {
            log.error("Validação {} não encontrada; mensagem descartada", validationId);
            return;
        }

        if (!dados.pendente()) {
            log.debug("Validação {} já resolvida; entrega ignorada", validationId);
            return;
        }

        validationService.registrarTentativa(validationId);

        var resultado = validationClient.validate(
                validationId,
                dados.imageKey(),
                dados.contentType(),
                dados.mission(),
                aiProperties.isWebhookEnabled() ? callbackUrl : null);

        if (resultado.isEmpty()) {
            throw new ValidacaoNaoConcluidaException(
                    "IA não respondeu para a validação " + validationId);
        }

        outcomeService.aplicar(validationId, resultado.get());
    }

    /** Falha temporária: a mensagem volta para a fila e depois para a DLQ. */
    public static class ValidacaoNaoConcluidaException extends RuntimeException {
        public ValidacaoNaoConcluidaException(String message) {
            super(message);
        }
    }
}
