package br.com.fiap.soulie.infra.messaging;

import br.com.fiap.soulie.domain.validation.Validation;
import br.com.fiap.soulie.domain.validation.ValidationOutcomeService;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.infra.ai.ImageValidationContract;
import br.com.fiap.soulie.infra.ai.ImageValidationClient;
import br.com.fiap.soulie.infra.ai.AiValidationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consome os pedidos de validacao, chama a IA e aplica o veredito.
 *
 * <p>E aqui que o fluxo deixa de ser sincrono: a requisicao do usuario ja
 * respondeu 202 ha algum tempo. O timeout de leitura do cliente HTTP e o que
 * impede um worker de ficar preso para sempre numa IA que nao responde.
 */
@Component
public class ValidationConsumer {

    private static final Logger log = LoggerFactory.getLogger(ValidationConsumer.class);

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ValidationOutcomeService outcomeService;

    @Autowired
    private ImageValidationClient validationClient;

    @Autowired
    private AiValidationProperties aiProperties;

    @Value("${soulie.ai.callback-url:}")
    private String callbackUrl;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consumir(ValidationMessage mensagem) {
        Long validationId = mensagem.validationId();
        log.debug("Processando validacao {}", validationId);

        Validation validation;
        try {
            validation = validationService.findById(validationId);
        } catch (RuntimeException exception) {
            // Validacao inexistente nao se resolve com retry: descartar e melhor
            // que ocupar o worker para sempre.
            log.error("Validacao {} nao encontrada; mensagem descartada", validationId);
            return;
        }

        // Reentrega de mensagem e comportamento normal do RabbitMQ. Se ja foi
        // resolvida, nao ha nada a fazer — e o ValidationService.resolve tambem
        // barraria, mas evitar a chamada paga da IA e o ponto desta checagem.
        if (!validation.isPending()) {
            log.debug("Validacao {} ja resolvida; entrega ignorada", validationId);
            return;
        }

        validationService.registrarTentativa(validationId);

        var evidence = validation.getEvidence();
        var mission = evidence.getDailyMission();
        var contexto = mission == null ? null : new ImageValidationContract.Mission(
                mission.getId(), mission.getName(), mission.getTag(),
                mission.getDescription(), mission.getEmoji());

        var resultado = validationClient.validate(
                validationId,
                evidence.getImageKey(),
                evidence.getContentType(),
                contexto,
                aiProperties.isWebhookEnabled() ? callbackUrl : null);

        if (resultado.isEmpty()) {
            // Sem resposta da IA. Lancar faz a mensagem voltar para a fila e, nas
            // tentativas esgotadas, cair na DLQ — de onde pode ser reprocessada
            // quando a IA voltar. Resolver como REJECTED aqui seria punir o
            // usuario por uma falha nossa.
            throw new ValidacaoNaoConcluidaException(
                    "IA nao respondeu para a validacao " + validationId);
        }

        outcomeService.aplicar(validationId, resultado.get());
    }

    /** Falha temporaria: a mensagem volta para a fila e depois para a DLQ. */
    public static class ValidacaoNaoConcluidaException extends RuntimeException {
        public ValidacaoNaoConcluidaException(String message) {
            super(message);
        }
    }
}
