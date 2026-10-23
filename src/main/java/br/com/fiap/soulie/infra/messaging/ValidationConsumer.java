package br.com.fiap.soulie.infra.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consome os pedidos de validação da fila e delega ao {@link ValidationProcessor}.
 */
@Component
@ConditionalOnProperty(name = "soulie.messaging.enabled", havingValue = "true")
public class ValidationConsumer {

    private final ValidationProcessor processor;

    public ValidationConsumer(ValidationProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consumir(ValidationMessage mensagem) {
        processor.processar(mensagem.validationId());
    }
}
