package br.com.fiap.soulie.validations.evidence;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import jakarta.validation.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MissionOpenValidatorTest {

    private final MissionOpenValidator validator = new MissionOpenValidator();

    private Mission missionWith(MissionStatus status) {
        return TestData.mission(1L, TestData.avatar(1L, TestData.user(1L)), TestData.action(1L, 10), status);
    }

    @ParameterizedTest
    @EnumSource(value = MissionStatus.class, names = {"ACTIVE", "IN_PROGRESS"})
    void acceptsEvidenceForOpenMission(MissionStatus status) {
        assertThatCode(() -> validator.validate(missionWith(status), 1L)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = MissionStatus.class, names = {"COMPLETED", "EXPIRED"})
    void rejectsEvidenceForClosedMission(MissionStatus status) {
        assertThatThrownBy(() -> validator.validate(missionWith(status), 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Esta missão não aceita mais evidências");
    }
}
