package br.com.fiap.soulie.infra.messaging;

import br.com.fiap.soulie.domain.validation.ValidationOutcomeService;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.infra.ai.AiValidationProperties;
import br.com.fiap.soulie.infra.ai.ImageValidationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consome os pedidos de validação, chama a IA e aplica o veredito.
 *
 * <p>Este método roda <b>fora de requisição web e fora de transação</b>. Com
 * {@code open-in-view: false}, isso proíbe manipular entidade carregada por
 * outro escopo: ela volta destacada e tocar numa associação {@code LAZY} estoura
 * {@code LazyInitializationException}. Por isso ele trabalha com o record de
 * {@code ValidationService.carregarParaAnalise}, e não com a entidade.
 *
 * <p>E por isso ele também <b>não</b> é {@code @Transactional}: a chamada à IA é
 * lenta e paga, e segurar uma conexão do pool durante ela esgotaria o pool sob
 * carga. As escritas acontecem em transações curtas, dentro dos services.
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
        log.debug("Processando validação {}", validationId);

        var dados = validationService.carregarParaAnalise(validationId).orElse(null);

        if (dados == null) {
            // Validação que não existe não se resolve com retry. Note que só
            // este caso é descartado: uma falha de banco (pool esgotado,
            // failover) propaga e vai para o retry, em vez de a evidência do
            // usuário sumir em silêncio.
            log.error("Validação {} não encontrada; mensagem descartada", validationId);
            return;
        }

        // Reentrega de mensagem é comportamento normal do RabbitMQ. O
        // ValidationService.resolve também barraria, mas evitar a chamada paga
        // da IA é o ponto desta checagem.
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
            // Sem resposta da IA. Lançar faz a mensagem voltar para a fila e,
            // esgotadas as tentativas, cair na DLQ — de onde pode ser
            // reprocessada quando a IA voltar. Resolver como REJECTED aqui
            // seria punir o usuário por uma falha nossa.
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
