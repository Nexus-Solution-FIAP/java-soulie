package br.com.fiap.soulie.infra.messaging;

import br.com.fiap.soulie.infra.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica o pedido de validação depois do commit.
 *
 * <p>Com Rabbit ligado, manda para a fila. Sem fila (perfil {@code dev}),
 * processa na hora no mesmo processo.
 */
@Component
public class ValidationPublisher {

    private static final Logger log = LoggerFactory.getLogger(ValidationPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final ValidationProcessor processor;
    private final boolean messagingEnabled;

    public ValidationPublisher(ObjectProvider<RabbitTemplate> rabbitTemplate,
                               @Lazy ValidationProcessor processor,
                               @Value("${soulie.messaging.enabled:false}") boolean messagingEnabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.processor = processor;
        this.messagingEnabled = messagingEnabled;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(ValidationRequestedEvent evento) {
        if (!messagingEnabled) {
            processor.processar(evento.validationId());
            return;
        }

        RabbitTemplate template = rabbitTemplate.getIfAvailable();
        if (template == null) {
            log.error("RabbitTemplate ausente; validação {} ficou PENDING", evento.validationId());
            return;
        }

        try {
            template.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ROUTING_KEY,
                    new ValidationMessage(evento.validationId()));
            log.debug("Validação {} publicada na fila", evento.validationId());
        } catch (AmqpException exception) {
            log.error("Não foi possível publicar a validação {} na fila. "
                    + "Ela está PENDING no banco e precisa ser reenfileirada.",
                    evento.validationId(), exception);
        }
    }

    /**
     * Confere que a fila está acessível ANTES de aceitar a evidência.
     *
     * <p>No modo local não há fila: o processamento roda depois do commit.
     */
    public void exigirFilaDisponivel() {
        if (!messagingEnabled) {
            return;
        }
        RabbitTemplate template = rabbitTemplate.getIfAvailable();
        if (template == null) {
            throw BusinessException.indisponivel(
                    "Não foi possível registrar sua evidência agora. Tente novamente em instantes.");
        }
        try {
            template.execute(canal -> canal.queueDeclarePassive(RabbitConfig.QUEUE));
        } catch (AmqpException exception) {
            log.error("Fila de validação indisponível", exception);
            throw BusinessException.indisponivel(
                    "Não foi possível registrar sua evidência agora. Tente novamente em instantes.");
        }
    }
}
