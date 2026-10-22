package br.com.fiap.soulie.infra.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fila de validação de evidências.
 *
 * <p>Topologia: uma exchange direta, uma fila de trabalho e uma dead-letter
 * queue. A DLQ não é detalhe de luxo — sem ela, uma mensagem que falha sempre
 * volta para a fila para sempre, consumindo worker e impedindo que as outras
 * avancem.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "soulie.validation";
    public static final String QUEUE = "soulie.validation.requested";
    public static final String ROUTING_KEY = "validation.requested";

    public static final String DLQ_EXCHANGE = "soulie.validation.dlx";
    public static final String DLQ = "soulie.validation.failed";
    public static final String DLQ_ROUTING_KEY = "validation.failed";

    @Bean
    public DirectExchange validationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange validationDeadLetterExchange() {
        return new DirectExchange(DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue validationQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLQ_EXCHANGE)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    /**
     * Fila morta: sem TTL, de propósito.
     *
     * <p>O que cai aqui é uma evidência de um usuário que fez a ação e não
     * conseguiu ser analisada. Expirar essas mensagens seria descartar trabalho
     * de pessoa real em silêncio — elas ficam para reprocessamento.
     */
    @Bean
    public Queue validationDeadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding validationBinding() {
        return BindingBuilder.bind(validationQueue()).to(validationExchange()).with(ROUTING_KEY);
    }

    @Bean
    public Binding validationDeadLetterBinding() {
        return BindingBuilder.bind(validationDeadLetterQueue())
                .to(validationDeadLetterExchange())
                .with(DLQ_ROUTING_KEY);
    }

    /** JSON em vez da serialização binária do Java, para a fila ser inspecionável. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
