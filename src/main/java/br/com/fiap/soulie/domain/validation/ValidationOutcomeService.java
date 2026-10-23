package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.community.UserNotification;
import br.com.fiap.soulie.domain.community.UserNotificationRepository;
import br.com.fiap.soulie.domain.mission.MissionApiService;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import br.com.fiap.soulie.infra.ai.ImageValidationContract;
import br.com.fiap.soulie.infra.push.ExpoPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

/**
 * O que acontece depois que a IA decide.
 *
 * <p>Separado do {@code ValidationService} porque são responsabilidades
 * distintas: um cuida do estado da validação, o outro das consequências dela
 * (recompensa, notificação, push). Juntar os dois faria o serviço de estado
 * depender de metade do domínio.
 *
 * <p>A recompensa **não é reimplementada aqui**: ela chama
 * {@code MissionApiService.rewardAprovada}, que é o mesmo caminho que já credita
 * XP, pontos, impacto, streak, atividade e conquistas. Duplicar essa lógica
 * seria garantir que as duas versões divergissem.
 */
@Service
public class ValidationOutcomeService {

    private static final Logger log = LoggerFactory.getLogger(ValidationOutcomeService.class);

    @Autowired
    private ValidationService validationService;

    @Autowired
    private MissionApiService missionApiService;

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Autowired
    private ExpoPushService expoPushService;

    /**
     * Aplica o veredito.
     *
     * <p>Tudo passa primeiro por {@link ValidationService#resolve}, que só deixa
     * passar uma vez. Se ele devolver vazio, outra entrega da mensagem já
     * cuidou desta validação, e nada mais acontece — nem recompensa, nem push.
     * É isso que impede a reentrega do RabbitMQ de creditar XP duas vezes.
     *
     * <p>{@code REQUIRES_NEW} porque, no perfil {@code dev}, o processor roda
     * no listener {@code AFTER_COMMIT} da transação que gravou a evidência. Ali
     * a transação original já acabou: um {@code @Modifying} (o
     * {@code resolveIfPending}) dispara {@code No active transaction}, a
     * análise da IA se perde e a evidência fica {@code PENDING}. Com fila
     * (thread do Rabbit) isso não aparece; sem fila, sim.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aplicar(Long validationId, ImageValidationContract.Result resultado) {
        Optional<Validation> resolvida = validationService.resolve(validationId, resultado);
        if (resolvida.isEmpty()) {
            return;
        }

        Validation validation = resolvida.get();
        var evidence = validation.getEvidence();
        User user = evidence.getUser();

        if (validation.getResultValidation() == ResultValidation.APPROVED) {
            if (evidence.isDoApp()) {
                missionApiService.rewardAprovada(user, evidence.getDailyMission());
            }
            notificar(user, "Missão confirmada!",
                    "A sua foto foi aprovada e o XP já está com você.");
            return;
        }

        if (validation.getResultValidation() == ResultValidation.REJECTED) {
            String motivo = validation.getReason() == null || validation.getReason().isBlank()
                    ? "A foto não comprova a missão."
                    : validation.getReason();
            notificar(user, "Foto não aprovada", motivo + " Você pode tentar de novo.");
            return;
        }

        // FAILED: problema nosso, não do usuário. A mensagem precisa deixar isso
        // claro, senão quem fez a ação certa entende que foi acusado de fraude.
        notificar(user, "Não conseguimos analisar agora",
                "Houve uma falha ao analisar a sua foto. Vamos tentar de novo em breve.");
        log.warn("Validação {} terminou como FAILED", validationId);
    }

    private void notificar(User user, String titulo, String mensagem) {
        // Trunca o texto que veio da IA antes de persistir e de exibir: a coluna
        // do corpo da notificacao tem 500 caracteres, e e texto de terceiro.
        String corpo = mensagem == null ? "" : mensagem.strip();
        if (corpo.length() > 400) {
            corpo = corpo.substring(0, 400);
        }
        var notification = notificationRepository.save(
                UserNotification.system(user, titulo, corpo));

        if (user.getExpoPushToken() == null || !Boolean.TRUE.equals(user.getNotificationsEnabled())) {
            return;
        }
        // O push sai DEPOIS do commit. Mandar aqui dentro seguraria o lock da
        // validacao e a conexao do pool durante a chamada HTTP ao Expo.
        var destino = user.getExpoPushToken();
        var corpoFinal = corpo;
        var notificationId = notification.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                expoPushService.send(destino, titulo, corpoFinal, notificationId);
            }
        });
    }
}
