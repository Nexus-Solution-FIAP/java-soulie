package br.com.fiap.soulie.domain.evidence;

import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.domain.validation.ValidationRepository;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import br.com.fiap.soulie.infra.messaging.ValidationDeadLetterConsumer;
import br.com.fiap.soulie.infra.messaging.ValidationMessage;
import br.com.fiap.soulie.infra.storage.StoredImage;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As guardas do envio de evidência, pelo caminho real e fora de transação.
 *
 * <p>Duas delas nasceram de achados graves de auditoria:
 *
 * <ul>
 *   <li><b>A trava permanente.</b> Quando a IA não respondia, a mensagem ia para
 *       a dead-letter queue e <b>ninguém escrevia FAILED</b> — a validação ficava
 *       {@code PENDING}, e pendência bloqueia reenvio. Dez minutos de
 *       indisponibilidade da IA deixavam todo usuário que enviou foto naquela
 *       janela permanentemente impedido de concluir aquela missão, sem
 *       notificação e com saída só por {@code UPDATE} manual.</li>
 *   <li><b>O teto de chamadas pagas.</b> {@code REJECTED} libera reenvio
 *       imediato e não havia contador: dava para reenviar a mesma foto em laço
 *       esperando um falso positivo da IA, cada ciclo custando uma chamada paga
 *       à API do parceiro.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class EvidenceSubmissionGuardTest {

    @Autowired private EvidenceSubmissionGuard guardas;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private ValidationRepository validationRepository;
    @Autowired private ValidationService validationService;
    @Autowired private DailyMissionRepository dailyMissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ValidationDeadLetterConsumer dlq;
    @Autowired private TransactionTemplate tx;

    private User user;
    private DailyMission mission;

    @BeforeEach
    void preparar() {
        tx.executeWithoutResult(status -> {
            user = userRepository.save(User.builder()
                    .name("Ana").email("guard-" + UUID.randomUUID() + "@soulie.com")
                    .password("$2a$10$abcdefghijklmnopqrstuv").active(true).points(0).build());
            mission = dailyMissionRepository.save(novaMissao());
        });
    }

    private DailyMission novaMissao() {
        var m = new DailyMission();
        ReflectionTestUtils.setField(m, "emoji", "🚲");
        ReflectionTestUtils.setField(m, "name", "Ir de bicicleta " + UUID.randomUUID());
        ReflectionTestUtils.setField(m, "tag", "Mobilidade");
        ReflectionTestUtils.setField(m, "description", "Trajeto de bicicleta");
        ReflectionTestUtils.setField(m, "xp", 40);
        ReflectionTestUtils.setField(m, "co2SavedKg", 0.4);
        ReflectionTestUtils.setField(m, "waterSavedLiters", 0.0);
        return m;
    }

    /** Cria uma evidência pendente e devolve o id da validação. */
    private Long enviarPendente() {
        return tx.execute(status -> guardas.registrarPendente(user, mission,
                new StoredImage("evidencias/2026-10-22/" + UUID.randomUUID() + ".jpg",
                        null, "image/jpeg", 1234)).validation().getId());
    }

    private ResultValidation estadoDe(Long validationId) {
        return tx.execute(s -> validationRepository.findById(validationId).orElseThrow()
                .getResultValidation());
    }

    @Test
    @DisplayName("Uma pendente por vez: o segundo envio e barrado")
    void pendenteBarraOSegundoEnvio() {
        enviarPendente();

        assertThatThrownBy(() -> guardas.conferirSePodeEnviar(user, mission))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("em análise");
    }

    @Test
    @DisplayName("TRAVA: pendencia orfa para de bloquear depois do corte")
    void pendenciaOrfaDeixaDeBloquear() {
        Long validationId = enviarPendente();

        // Simula o que acontece quando a publicação se perde entre o commit e a
        // fila, ou o processo morre no meio: a pendência existe e nada vai
        // resolvê-la. Sem o corte por tempo, a missão ficaria trancada para
        // sempre.
        tx.executeWithoutResult(s -> {
            var validation = validationRepository.findById(validationId).orElseThrow();
            ReflectionTestUtils.setField(validation, "requestedAt",
                    LocalDateTime.now().minusHours(2));
            validationRepository.save(validation);
        });

        assertThatCode(() -> guardas.conferirSePodeEnviar(user, mission))
                .as("pendência velha não pode virar cadeado")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TRAVA: o consumidor da DLQ resolve FAILED e libera a missao")
    void consumidorDaDlqResolveFailedELibera() {
        Long validationId = enviarPendente();
        assertThat(estadoDe(validationId)).isEqualTo(ResultValidation.PENDING);

        // É este o caminho que não existia: a mensagem esgotava as tentativas,
        // caía na DLQ e morria lá, deixando PENDING para sempre.
        dlq.consumir(new ValidationMessage(validationId));

        assertThat(estadoDe(validationId))
                .as("FAILED, não REJECTED: ninguém é punido por falha nossa")
                .isEqualTo(ResultValidation.FAILED);

        assertThatCode(() -> guardas.conferirSePodeEnviar(user, mission))
                .as("resolvida a pendência, a missão volta a aceitar envio")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("O consumidor da DLQ e idempotente e nao estoura em id inexistente")
    void consumidorDaDlqEhDefensivo() {
        Long validationId = enviarPendente();

        dlq.consumir(new ValidationMessage(validationId));
        assertThatCode(() -> dlq.consumir(new ValidationMessage(validationId)))
                .doesNotThrowAnyException();
        assertThat(estadoDe(validationId)).isEqualTo(ResultValidation.FAILED);

        // Relançar aqui descartaria a mensagem em silêncio, porque a DLQ não tem
        // dead-letter própria.
        assertThatCode(() -> dlq.consumir(new ValidationMessage(999_999L)))
                .doesNotThrowAnyException();
        assertThatCode(() -> dlq.consumir(new ValidationMessage(null)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TETO: o quarto envio do dia e recusado, mesmo tudo resolvido")
    void tetoDeEnviosPorDia() {
        // Cada ciclo resolvido libera o próximo, que é o que permitia insistir
        // até a IA aprovar por acaso. O teto é o que fecha isso.
        for (int i = 1; i <= 3; i++) {
            Long id = enviarPendente();
            tx.executeWithoutResult(s ->
                    validationRepository.resolveIfPending(id, ResultValidation.REJECTED));
        }

        assertThatThrownBy(() -> guardas.conferirSePodeEnviar(user, mission))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("hoje");
    }

    @Test
    @DisplayName("O teto e por missao: outra missao continua liberada")
    void tetoEhPorMissao() {
        for (int i = 1; i <= 3; i++) {
            Long id = enviarPendente();
            tx.executeWithoutResult(s ->
                    validationRepository.resolveIfPending(id, ResultValidation.REJECTED));
        }

        DailyMission outra = tx.execute(s -> dailyMissionRepository.save(novaMissao()));

        assertThatCode(() -> guardas.conferirSePodeEnviar(user, outra))
                .doesNotThrowAnyException();
    }
}
