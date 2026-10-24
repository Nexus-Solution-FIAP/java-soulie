package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.history_points.HistoryPointsRepository;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionNotDoneTodayValidatorTest {

    @Mock
    private HistoryPointsRepository historyPointsRepository;

    @InjectMocks
    private ActionNotDoneTodayValidator validator;

    private final Mission mission = TestData.mission(1L, TestData.avatar(1L, TestData.user(1L)),
            TestData.action(3L, 10), MissionStatus.IN_PROGRESS);

    @Test
    void passesWhenActionNotScoredToday() {
        when(historyPointsRepository.existsByActionIdAndUserIdAndDate(3L, 1L, LocalDate.now())).thenReturn(false);

        assertThatCode(() -> validator.validate(mission, 1L)).doesNotThrowAnyException();
    }

    @Test
    void failsWhenActionAlreadyScoredToday() {
        when(historyPointsRepository.existsByActionIdAndUserIdAndDate(3L, 1L, LocalDate.now())).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(mission, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Você já pontuou com esta ação hoje");
    }
}
