package br.com.fiap.soulie.domain.activity;

import br.com.fiap.soulie.api.dto.CreateActivityRequest;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.security.TokenService;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /v1/users/me/activities} vendia XP a quem pedisse.
 *
 * <p>Eram dois caminhos, e a correção anterior fechou só um. O que ela fechou:
 * o texto livre era casado com o nome da missão, concedendo a recompensa cheia
 * sem foto e sem IA. O que <b>ficou aberto</b>: o {@code xp} continuava vindo do
 * cliente e indo para {@code addPoints}/{@code addXp}, limitado a 50 <b>por
 * requisição</b> — e o número de requisições nunca teve limite. Medido pelo
 * endpoint real: 5 chamadas de 50 davam 250 pontos, com nível, streak e
 * conquista. O pipeline de IA continuava opcional.
 *
 * <p><b>E esta classe de teste passava.</b> Três dos quatro testes chamavam o
 * {@code Validator} direto, provando que a anotação {@code @Max(50)} existe — o
 * que não diz nada sobre o endpoint aplicá-la, nem sobre o total acumulado. Se
 * alguém removesse o {@code @Valid} do controller, eles continuariam verdes.
 * Agora os casos atravessam HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityXpBypassTest {

    @Autowired private MockMvc mvc;
    @Autowired private ActivityApiService activityApiService;
    @Autowired private DailyMissionRepository dailyMissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;

    private String tokenDe(String email) {
        var user = userRepository.save(User.builder()
                .name("Bypass").email(email).password("$2a$10$abcdefghijklmnopqrstuv")
                .points(0).active(true).build());
        return tokenService.generateToken(user);
    }

    private int pontosDe(String email) {
        return userRepository.findByEmail(email).getPoints();
    }

    private String corpo(int xp, String rotulo) {
        return "{\"emoji\":\"X\",\"label\":\"%s\",\"xp\":%d}".formatted(rotulo, xp);
    }

    @Test
    @DisplayName("O endpoint aplica o teto por requisicao: xp absurdo da 400")
    void endpointAplicaOTetoPorRequisicao() throws Exception {
        // Prova o @Valid no controller, não só a anotação no DTO: era isto que
        // estourava o int numa requisição e virava o saldo negativo.
        String email = "teto.request@soulie.com";

        mvc.perform(post("/v1/users/me/activities")
                        .header("Authorization", "Bearer " + tokenDe(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(2_000_000_000, "estouro")))
                .andExpect(status().isBadRequest());

        assertThat(pontosDe(email)).isZero();
    }

    @Test
    @DisplayName("O endpoint recusa xp negativo")
    void endpointRecusaXpNegativo() throws Exception {
        String email = "negativo@soulie.com";

        mvc.perform(post("/v1/users/me/activities")
                        .header("Authorization", "Bearer " + tokenDe(email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(-50, "negativo")))
                .andExpect(status().isBadRequest());

        assertThat(pontosDe(email)).isZero();
    }

    @Test
    @DisplayName("BYPASS: repetir o pedido nao acumula XP alem do teto do dia")
    void repetirNaoAcumulaAlemDoTetoDiario() throws Exception {
        // ESTE é o teste que faltava. Antes da correção: 50, 100, 150, 200, 250.
        String email = "acumulo@soulie.com";
        String token = tokenDe(email);

        for (int i = 1; i <= 5; i++) {
            mvc.perform(post("/v1/users/me/activities")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo(50, "caminhei um pouco " + i)))
                    .andExpect(status().isOk());
        }

        assertThat(pontosDe(email))
                .as("o teto de XP livre é DIÁRIO e agregado, não por requisição")
                .isEqualTo(50);
    }

    @Test
    @DisplayName("Passado o teto do dia, a resposta diz que o XP concedido foi zero")
    void respostaDizAVerdadeSobreOXpConcedido() throws Exception {
        String email = "verdade@soulie.com";
        String token = tokenDe(email);

        mvc.perform(post("/v1/users/me/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(50, "primeira")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp").value(50));

        // A resposta tem de refletir o que entrou no saldo. Devolver o valor
        // pedido faria o app mostrar um número que o saldo não tem.
        mvc.perform(post("/v1/users/me/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(50, "segunda")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp").value(0));
    }

    @Test
    @DisplayName("Atividade que menciona uma missao e recusada: missao exige foto")
    void atividadeComNomeDeMissaoEhRecusada() {
        var user = userRepository.save(User.builder()
                .name("Bypass").email("bypass@soulie.com").password("hash")
                .points(0).active(true).build());
        var mission = dailyMissionRepository.save(missaoDeBicicleta());
        int pontosAntes = user.getPoints();

        assertThatThrownBy(() -> activityApiService.create(
                user, new CreateActivityRequest("🚲", "Ir de bicicleta realizada", 10)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("/missions/%d/validate".formatted(mission.getId()));

        // E, o que mais importa: nenhum XP foi creditado por esse caminho.
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPoints())
                .isEqualTo(pontosAntes);
    }

    private DailyMission missaoDeBicicleta() {
        var mission = new DailyMission();
        ReflectionTestUtils.setField(mission, "emoji", "🚲");
        ReflectionTestUtils.setField(mission, "name", "Ir de bicicleta");
        ReflectionTestUtils.setField(mission, "tag", "Mobilidade");
        ReflectionTestUtils.setField(mission, "description", "Trajeto de bicicleta");
        ReflectionTestUtils.setField(mission, "xp", 40);
        ReflectionTestUtils.setField(mission, "co2SavedKg", 0.4);
        ReflectionTestUtils.setField(mission, "waterSavedLiters", 0.0);
        return mission;
    }
}
