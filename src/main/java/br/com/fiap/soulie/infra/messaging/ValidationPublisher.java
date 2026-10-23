package br.com.fiap.soulie.infra.messaging;

import br.com.fiap.soulie.infra.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica o pedido de validação na fila.
 *
 * <p><b>A publicação acontece em {@code AFTER_COMMIT}, e isso não é detalhe de
 * estilo.</b> Publicar dentro da transação é uma corrida clássica: o worker é
 * rápido, consome a mensagem antes do commit e não encontra a validação no
 * banco — a evidência fica PENDING para sempre, e o bug só aparece sob carga,
 * que é quando ninguém quer descobri-lo.
 *
 * <p>Por isso o serviço dispara um {@link ValidationRequestedEvent} e quem
 * publica é este listener, depois que a transação confirmou.
 */
@Component
public class ValidationPublisher {

    private static final Logger log = LoggerFactory.getLogger(ValidationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ValidationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(ValidationRequestedEvent evento) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ROUTING_KEY,
                    new ValidationMessage(evento.validationId()));
            log.debug("Validação {} publicada na fila", evento.validationId());
        } catch (AmqpException exception) {
            // Aqui a transação já foi confirmada: a evidência está salva e a
            // validação está PENDING. Não dá para devolver erro ao cliente, e
            // também não se perde nada — a validação fica no banco e pode ser
            // reenfileirada. Ver docs/08-operacao.md.
            log.error("Não foi possível publicar a validação {} na fila. "
                    + "Ela está PENDING no banco e precisa ser reenfileirada.",
                    evento.validationId(), exception);
        }
    }

    /**
     * Confere que a fila está acessível ANTES de aceitar a evidência.
     *
     * <p>Chamado pelo serviço no início da operação. Serve para o cliente
     * receber 503 com mensagem clara em vez de um 202 que promete uma análise
     * que nunca vai acontecer.
     */
    public void exigirFilaDisponivel() {
        try {
            rabbitTemplate.execute(canal -> canal.queueDeclarePassive(RabbitConfig.QUEUE));
        } catch (AmqpException exception) {
            log.error("Fila de validação indisponível", exception);
            throw BusinessException.indisponivel(
                    "Não foi possível registrar sua evidência agora. Tente novamente em instantes.");
        }
    }
}
