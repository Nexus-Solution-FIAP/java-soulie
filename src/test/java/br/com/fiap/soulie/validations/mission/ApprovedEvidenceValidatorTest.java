package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.validation.ValidationRepository;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovedEvidenceValidatorTest {

    @Mock
    private ValidationRepository validationRepository;

    @InjectMocks
    private ApprovedEvidenceValidator validator;

    private final Mission mission = TestData.mission(7L, TestData.avatar(1L, TestData.user(1L)),
            TestData.action(1L, 10), MissionStatus.IN_PROGRESS);

    @Test
    void passesWhenAiApprovedAnEvidence() {
        when(validationRepository.existsByEvidenceMissionIdAndResultValidation(7L, ResultValidation.APPROVED))
                .thenReturn(true);

        assertThatCode(() -> validator.validate(mission, 1L)).doesNotThrowAnyException();
    }

    @Test
    void failsWithoutApprovedEvidence() {
        when(validationRepository.existsByEvidenceMissionIdAndResultValidation(7L, ResultValidation.APPROVED))
                .thenReturn(false);

        assertThatThrownBy(() -> validator.validate(mission, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("A missão ainda não possui evidência aprovada");
    }
}
