package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.validations.evidence.EvidenceSubmissionValidator;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MissionOwnerValidatorTest {

    private final MissionOwnerValidator validator = new MissionOwnerValidator();

    private Mission missionOfUser(Long userId) {
        return TestData.mission(1L, TestData.avatar(1L, TestData.user(userId)),
                TestData.action(1L, 10), MissionStatus.ACTIVE);
    }

    @Test
    void passesForMissionOwner() {
        assertThatCode(() -> validator.validate(missionOfUser(1L), 1L)).doesNotThrowAnyException();
    }

    @Test
    void failsForAnotherUser() {
        assertThatThrownBy(() -> validator.validate(missionOfUser(1L), 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Esta missão não pertence ao seu avatar");
    }

    @Test
    void isUsedByBothFlows() {
        assertThat(validator)
                .isInstanceOf(MissionCompletionValidator.class)
                .isInstanceOf(EvidenceSubmissionValidator.class);
    }
}
