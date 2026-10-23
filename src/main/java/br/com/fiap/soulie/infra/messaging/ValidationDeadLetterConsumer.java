package br.com.fiap.soulie.infra.messaging;

import br.com.fiap.soulie.domain.validation.ValidationOutcomeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Resolve como FAILED o que a IA não conseguiu analisar.
 *
 * <p><b>Sem este consumidor o sistema trancava a missão do usuário para
 * sempre.</b> Quando a IA não respondia, o {@link ValidationConsumer} lançava,
 * a mensagem esgotava as tentativas e caía na dead-letter queue — e ninguém
 * escrevia {@code FAILED}. A {@code Validation} ficava {@code PENDING}, e o
 * {@code existsPendingForDailyMission} então barrava qualquer reenvio daquela
 * missão. Dez minutos de indisponibilidade da IA bastavam para deixar todo
 * usuário que enviou foto naquela janela permanentemente impedido de concluir
 * aquela missão, sem nenhuma notificação, e com saída só por {@code UPDATE}
 * manual no banco.
 *
 * <p>A documentação já descrevia este comportamento — "esgotado → DLQ →
 * FAILED" — e a transição não existia em lugar nenhum. Aqui ela existe.
 *
 * <p>Resolver como {@code FAILED} é o que faz as três coisas certas ao mesmo
 * tempo: o usuário recebe o push dizendo que não conseguimos analisar, a missão
 * volta a aceitar envio (o bloqueio é só de {@code PENDING}) e a linha fica no
 * banco com {@code attempts} para quem quiser reprocessar depois. {@code FAILED}
 * não é {@code REJECTED}: ninguém é punido por uma falha nossa.
 */
@Component
public class ValidationDeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(ValidationDeadLetterConsumer.class);

    private final ValidationOutcomeService outcomeService;

    public ValidationDeadLetterConsumer(ValidationOutcomeService outcomeService) {
        this.outcomeService = outcomeService;
    }

    @RabbitListener(queues = RabbitConfig.DLQ)
    public void consumir(ValidationMessage mensagem) {
        Long validationId = mensagem == null ? null : mensagem.validationId();
        if (validationId == null) {
            log.error("Mensagem sem validationId na dead-letter queue; descartada");
            return;
        }

        try {
            // Resultado nulo traduz para FAILED e dispara a notificação. O
            // resolve é idempotente (UPDATE condicional conferindo rowcount),
            // então uma validação que já tenha sido resolvida por outro caminho
            // não é alterada.
            outcomeService.aplicar(validationId, null);
            log.warn("Validação {} resolvida como FAILED: a IA não concluiu a análise", validationId);
        } catch (RuntimeException exception) {
            // Não relança. Esta é a última parada da mensagem: a DLQ não tem
            // dead-letter própria, e com default-requeue-rejected=false relançar
            // aqui descartaria a mensagem em silêncio — exatamente o que este
            // consumidor existe para evitar. Falhando, fica o log para
            // investigação e a mensagem é confirmada.
            log.error("Falha ao resolver como FAILED a validação {}", validationId, exception);
        }
    }
}
