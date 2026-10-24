package br.com.fiap.soulie.infra.ai;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.evidence.EvidenceRepository;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.domain.validation.ValidationRepository;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O webhook pelo caminho real: requisição HTTP atravessando a cadeia de filtros.
 *
 * <p><b>Esta cobertura não existia.</b> O `HmacSignatureTest` testa a classe de
 * assinatura isolada, e isso é útil, mas o controller tem
 * {@code @ConditionalOnProperty(soulie.ai.webhook-enabled=true)} e o perfil de
 * teste deixa a propriedade em {@code false} — então o bean <b>nunca subia em
 * teste nenhum</b>. O endpoint mais sensível do projeto, que é público e
 * credita XP, tinha zero teste de integração.
 *
 * <p>Aqui a propriedade é ligada explicitamente, e cada caso é um jeito
 * diferente de tentar creditar XP sem autorização.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "soulie.ai.webhook-enabled=true",
        "soulie.ai.webhook-secret=segredo-de-teste-do-webhook-com-tamanho-suficiente"
})
class ValidationWebhookControllerTest {

    private static final String SEGREDO = "segredo-de-teste-do-webhook-com-tamanho-suficiente";

    @Autowired private MockMvc mvc;
    @Autowired private HmacSignature hmac;
    @Autowired private UserRepository userRepository;
    @Autowired private DailyMissionRepository dailyMissionRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private ValidationRepository validationRepository;
    @Autowired private ValidationService validationService;
    @Autowired private TransactionTemplate tx;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private Long validationId;
    private Long userId;

    @BeforeEach
    void prepararValidacaoPendente() {
        var ids = tx.execute(status -> {
            User user = userRepository.save(User.builder()
                    .name("Ana").email("ana.webhook@soulie.com")
                    .password("$2a$10$abcdefghijklmnopqrstuv").active(true)
                    .points(0).build());
            DailyMission mission = dailyMissionRepository.findById(inserirMissao()).orElseThrow();
            Evidence evidence = evidenceRepository.save(
                    Evidence.doAppParaMissaoDiaria(user, mission,
                            new br.com.fiap.soulie.infra.storage.StoredImage(
                                    "evidencias/2026-10-22/abc.jpg", null, "image/jpeg", 1234)));
            var validation = validationService.createPending(evidence);
            return new long[]{validation.getId(), user.getId()};
        });
        validationId = ids[0];
        userId = ids[1];
    }

    /**
     * Uma missão do catálogo, inserida por SQL.
     *
     * <p>{@code DailyMission} não tem {@code @Builder} nem setter, e acrescentar
     * um deles a uma entidade de produção só para montar este cenário seria
     * mudança de código de produção dentro de um PR de segurança. A linha entra
     * direto e depois é carregada pelo repository.
     */
    private Long inserirMissao() {
        jdbc.update("""
                insert into "daily_missions"
                       ("emoji", "name", "tag", "description", "xp",
                        "co2_saved_kg", "water_saved_liters", "period")
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, "B", "Ir de bicicleta", "mobilidade", "Use a bicicleta no trajeto",
                40, 1.2, 0.0, "DAILY");
        return jdbc.queryForObject(
                "select max(\"id\") from \"daily_missions\"", Long.class);
    }

    private String corpoAprovando() {
        return "{\"validationId\":%d,\"status\":\"APPROVED\",\"detections\":[\"bicicleta\"],\"confidence\":0.9}"
                .formatted(validationId);
    }

    private MockHttpServletRequestBuilder chamada(String corpo, String timestamp, String assinatura) {
        var pedido = post("/webhooks/validations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
        if (timestamp != null) {
            pedido = pedido.header("X-Soulie-Timestamp", timestamp);
        }
        if (assinatura != null) {
            pedido = pedido.header("X-Soulie-Signature", assinatura);
        }
        return pedido;
    }

    private MockHttpServletRequestBuilder chamadaAssinada(String corpo, String timestamp) {
        return chamada(corpo, timestamp, hmac.calcular(SEGREDO, timestamp, corpo));
    }

    private String agora() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    private ResultValidation estado() {
        return tx.execute(s -> validationRepository.findById(validationId).orElseThrow().getResultValidation());
    }

    private int pontos() {
        return tx.execute(s -> userRepository.findById(userId).orElseThrow().getPoints());
    }

    @Test
    @DisplayName("Assinatura valida credita XP e resolve a validacao")
    void assinaturaValidaCredita() throws Exception {
        String corpo = corpoAprovando();

        mvc.perform(chamadaAssinada(corpo, agora())).andExpect(status().isNoContent());

        assertThat(estado()).isEqualTo(ResultValidation.APPROVED);
        assertThat(pontos()).as("o XP da missão tem de ter sido creditado").isPositive();
    }

    @Test
    @DisplayName("BYPASS: sem assinatura nao credita")
    void semAssinaturaNaoCredita() throws Exception {
        mvc.perform(chamada(corpoAprovando(), agora(), null))
                .andExpect(status().isUnauthorized());

        assertThat(estado()).isEqualTo(ResultValidation.PENDING);
        assertThat(pontos()).isZero();
    }

    @Test
    @DisplayName("BYPASS: assinatura de outro segredo nao credita")
    void assinaturaDeOutroSegredoNaoCredita() throws Exception {
        String corpo = corpoAprovando();
        String ts = agora();

        mvc.perform(chamada(corpo, ts, hmac.calcular("outro-segredo-qualquer-mas-longo-o-bastante", ts, corpo)))
                .andExpect(status().isUnauthorized());

        assertThat(estado()).isEqualTo(ResultValidation.PENDING);
        assertThat(pontos()).isZero();
    }

    @Test
    @DisplayName("BYPASS: assinatura que cobre so o corpo nao credita")
    void assinaturaSemTimestampNaoCredita() throws Exception {
        // Se o timestamp não entrasse no material assinado, uma assinatura
        // capturada poderia ser reenviada para sempre, só trocando o cabeçalho
        // de hora. Este teste é o que trava isso.
        String corpo = corpoAprovando();

        mvc.perform(chamada(corpo, agora(), hmac.calcular(SEGREDO, "", corpo)))
                .andExpect(status().isUnauthorized());

        assertThat(estado()).isEqualTo(ResultValidation.PENDING);
    }

    @Test
    @DisplayName("BYPASS: corpo adulterado depois de assinado nao credita")
    void corpoAdulteradoNaoCredita() throws Exception {
        String corpoOriginal = "{\"validationId\":%d,\"status\":\"REJECTED\"}".formatted(validationId);
        String ts = agora();
        String assinatura = hmac.calcular(SEGREDO, ts, corpoOriginal);

        // Mesma assinatura, corpo trocado para APPROVED.
        mvc.perform(chamada(corpoAprovando(), ts, assinatura))
                .andExpect(status().isUnauthorized());

        assertThat(estado()).isEqualTo(ResultValidation.PENDING);
        assertThat(pontos()).isZero();
    }

    @Test
    @DisplayName("ANTI-REPLAY: timestamp velho e recusado mesmo bem assinado")
    void timestampVelhoEhRecusado() throws Exception {
        String velho = String.valueOf(Instant.now().minus(Duration.ofHours(1)).getEpochSecond());

        mvc.perform(chamadaAssinada(corpoAprovando(), velho))
                .andExpect(status().isUnauthorized());

        assertThat(estado()).isEqualTo(ResultValidation.PENDING);
    }

    @Test
    @DisplayName("ANTI-REPLAY: timestamp no futuro tambem e recusado")
    void timestampNoFuturoEhRecusado() throws Exception {
        String futuro = String.valueOf(Instant.now().plus(Duration.ofHours(1)).getEpochSecond());

        mvc.perform(chamadaAssinada(corpoAprovando(), futuro))
                .andExpect(status().isUnauthorized());

        assertThat(estado()).isEqualTo(ResultValidation.PENDING);
    }

    @Test
    @DisplayName("IDEMPOTENCIA: repetir a chamada valida nao credita duas vezes")
    void repetirNaoCreditaDuasVezes() throws Exception {
        String corpo = corpoAprovando();
        String ts = agora();

        mvc.perform(chamadaAssinada(corpo, ts)).andExpect(status().isNoContent());
        int depoisDaPrimeira = pontos();

        mvc.perform(chamadaAssinada(corpo, ts)).andExpect(status().isNoContent());

        assertThat(pontos())
                .as("a segunda entrega da mesma validação não pode creditar de novo")
                .isEqualTo(depoisDaPrimeira);
    }

    @Test
    @DisplayName("Id inexistente responde igual ao sucesso, sem oraculo")
    void idInexistenteNaoRevelaNada() throws Exception {
        String corpo = "{\"validationId\":999999,\"status\":\"APPROVED\"}";

        // Mesma resposta do caminho de sucesso: quem tiver o segredo não
        // consegue descobrir quais ids existem varrendo.
        mvc.perform(chamadaAssinada(corpo, agora())).andExpect(status().isNoContent());

        assertThat(estado()).isEqualTo(ResultValidation.PENDING);
    }
}
