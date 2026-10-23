package br.com.fiap.soulie.domain.evidence;

import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.infra.storage.StoredImage;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * As transações curtas do envio de evidência.
 *
 * <p>É um bean separado por um motivo mecânico, não estético: o
 * {@code EvidenceService.submitForDailyMission} deixou de ser
 * {@code @Transactional} para que o upload de até 10MB não segure uma conexão
 * do pool, e {@code @Transactional} <b>não se aplica a chamada de método da
 * própria classe</b> — o proxy do Spring fica de fora. Se estes dois métodos
 * morassem lá, as anotações seriam silenciosamente ignoradas, que é a pior
 * forma de errar isso.
 */
@Component
public class EvidenceSubmissionGuard {

    /**
     * Quanto tempo uma validação {@code PENDING} continua bloqueando a missão.
     *
     * <p>Passado isso ela é considerada órfã. O retry e o backoff do consumidor
     * cabem em poucos minutos, então 15 é folgado para o caminho legítimo e
     * curto o bastante para o usuário não ficar preso por um erro nosso.
     */
    private static final Duration PENDENCIA_CONSIDERADA_VIVA = Duration.ofMinutes(15);

    /** Envios por missão por dia. Cada um é uma chamada paga à IA. */
    static final int MAXIMO_DE_ENVIOS_POR_DIA = 3;

    private final EvidenceRepository evidenceRepository;
    private final ValidationService validationService;

    public EvidenceSubmissionGuard(EvidenceRepository evidenceRepository,
                                   ValidationService validationService) {
        this.evidenceRepository = evidenceRepository;
        this.validationService = validationService;
    }

    /**
     * As duas guardas, numa transação curta de leitura.
     *
     * <p>A primeira — uma pendente por vez — impede o laço imediato. A segunda
     * — teto de envios no dia — é a que impede o laço lento: sem ela,
     * {@code REJECTED} libera reenvio na hora e dava para insistir até a IA
     * aprovar por acaso, cada tentativa custando uma chamada paga.
     */
    @Transactional(readOnly = true)
    public void conferirSePodeEnviar(User user, DailyMission mission) {
        var agora = LocalDateTime.now();

        if (evidenceRepository.existsPendingForDailyMission(
                user.getId(), mission.getId(), agora.minus(PENDENCIA_CONSIDERADA_VIVA))) {
            throw new ValidationException(
                    "Você já tem uma foto desta missão em análise. Aguarde o resultado.");
        }

        long enviosHoje = evidenceRepository.contarEnviosNaJanela(
                user.getId(), mission.getId(), agora.toLocalDate().atStartOfDay());
        if (enviosHoje >= MAXIMO_DE_ENVIOS_POR_DIA) {
            throw new ValidationException(
                    "Você já enviou %d fotos desta missão hoje. Tente novamente amanhã."
                            .formatted(MAXIMO_DE_ENVIOS_POR_DIA));
        }
    }

    /** A escrita, em transação própria e curta, com o upload já concluído. */
    @Transactional
    public EvidenceService.EvidenciaRegistrada registrarPendente(User user,
                                                                 DailyMission mission,
                                                                 StoredImage imagem) {
        var evidence = evidenceRepository.save(Evidence.doAppParaMissaoDiaria(user, mission, imagem));
        var validation = validationService.createPending(evidence);
        return new EvidenceService.EvidenciaRegistrada(evidence, validation);
    }
}
