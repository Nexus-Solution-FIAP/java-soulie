package br.com.fiap.soulie.domain.activity;

import br.com.fiap.soulie.api.dto.CreateActivityRequest;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * `POST /v1/me/activities` contornava o pipeline inteiro de validação por IA.
 *
 * <p>Dois caminhos: o {@code xp} vinha do cliente sem teto e ia direto para
 * {@code addPoints}/{@code addXp}; e o texto livre era casado com o nome da
 * missão, concedendo a recompensa cheia <b>sem foto e sem IA</b>. Enquanto essa
 * porta existisse, todo o esforço da Fase 4 era decorativo.
 */
@SpringBootTest
@ActiveProfiles("test")
class ActivityXpBypassTest {

    @Autowired private ActivityApiService activityApiService;
    @Autowired private DailyMissionRepository dailyMissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private Validator validator;

    @Test
    @DisplayName("XP acima do teto é recusado pela validação do DTO")
    void xpAcimaDoTetoEhRecusado() {
        // Era isto que estourava o int numa requisição e virava o saldo negativo.
        var exploit = new CreateActivityRequest("💥", "Atividade qualquer", 2_000_000_000);

        assertThat(validator.validate(exploit))
                .isNotEmpty()
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo("xp"));
    }

    @Test
    @DisplayName("XP negativo é recusado")
    void xpNegativoEhRecusado() {
        assertThat(validator.validate(new CreateActivityRequest("💥", "x", -50))).isNotEmpty();
    }

    @Test
    @DisplayName("XP dentro do teto passa")
    void xpDentroDoTetoPassa() {
        assertThat(validator.validate(new CreateActivityRequest("🚶", "Caminhei", 20))).isEmpty();
    }

    @Test
    @DisplayName("Atividade que menciona uma missão é recusada: missão exige foto")
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
