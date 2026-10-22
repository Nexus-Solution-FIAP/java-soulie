package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.mission.dto.MissionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MissionTest {

    @Test
    @DisplayName("Missão nova nasce ACTIVE")
    void newMissionIsActive() {
        var avatar = TestData.avatar(1L, TestData.user(1L));
        var action = TestData.action(1L, 30);

        var mission = new Mission(new MissionRequest(1L, "Regar a planta"), action, avatar);

        assertThat(mission.getStatus()).isEqualTo(MissionStatus.ACTIVE);
        assertThat(mission.getDescription()).isEqualTo("Regar a planta");
        assertThat(mission.getAvatar()).isSameAs(avatar);
        assertThat(mission.getAction()).isSameAs(action);
    }

    @Test
    @DisplayName("startValidation coloca a missão IN_PROGRESS")
    void startValidation() {
        var mission = TestData.mission(1L, TestData.avatar(1L, TestData.user(1L)),
                TestData.action(1L, 30), MissionStatus.ACTIVE);

        mission.startValidation();

        assertThat(mission.getStatus()).isEqualTo(MissionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("complete marca COMPLETED e dá o XP da ação ao avatar")
    void completeGivesExperience() {
        var avatar = TestData.avatar(1L, TestData.user(1L));
        var mission = TestData.mission(1L, avatar, TestData.action(1L, 130), MissionStatus.IN_PROGRESS);

        mission.complete();

        assertThat(mission.getStatus()).isEqualTo(MissionStatus.COMPLETED);
        // O avatar comeca com xp 0 e ganha os 130 pontos da acao.
        // No java-soulie `xp` e o TOTAL acumulado e o nivel vem de
        // SoulieLevel.levelOf(xp) -> 130 esta na faixa [100, 200), ou seja nivel 2.
        // (No soulie-soul-up o campo guardava o XP DENTRO do nivel, entao o valor
        // esperado la era 30: 130 - 100 consumidos ao subir de nivel.)
        assertThat(avatar.getLevel()).isEqualTo(2);
        assertThat(avatar.getXp()).isEqualTo(130);
    }
}
