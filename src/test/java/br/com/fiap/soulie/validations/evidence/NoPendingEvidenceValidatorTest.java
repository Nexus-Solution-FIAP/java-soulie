package br.com.fiap.soulie.validations.evidence;

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
class NoPendingEvidenceValidatorTest {

    @Mock
    private ValidationRepository validationRepository;

    @InjectMocks
    private NoPendingEvidenceValidator validator;

    private final Mission mission = TestData.mission(4L, TestData.avatar(1L, TestData.user(1L)),
            TestData.action(1L, 10), MissionStatus.IN_PROGRESS);

    @Test
    void passesWhenNoEvidenceIsPending() {
        when(validationRepository.existsByEvidenceMissionIdAndResultValidation(4L, ResultValidation.PENDING))
                .thenReturn(false);

        assertThatCode(() -> validator.validate(mission, 1L)).doesNotThrowAnyException();
    }

    @Test
    void failsWhenAnEvidenceIsPending() {
        when(validationRepository.existsByEvidenceMissionIdAndResultValidation(4L, ResultValidation.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> validator.validate(mission, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Já existe uma evidência aguardando validação para esta missão");
    }
}
