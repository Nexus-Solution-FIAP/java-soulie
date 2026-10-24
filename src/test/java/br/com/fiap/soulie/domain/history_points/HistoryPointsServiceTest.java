package br.com.fiap.soulie.domain.history_points;

import br.com.fiap.soulie.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryPointsServiceTest {

    @Mock
    private HistoryPointsRepository historyPointsRepository;

    @InjectMocks
    private HistoryPointsService service;

    @Test
    void registerSavesTodayWithPoints() {
        var user = TestData.user(1L);
        var action = TestData.action(2L, 30);

        service.register(user, action, 30);

        var captor = ArgumentCaptor.forClass(HistoryPoints.class);
        verify(historyPointsRepository).save(captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.now());
        assertThat(captor.getValue().getPointsEarned()).isEqualTo(30);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getAction()).isSameAs(action);
    }

    @Test
    void myHistoryMapsToResponse() {
        var history = new HistoryPoints(TestData.user(1L), TestData.action(2L, 30), 30);
        when(historyPointsRepository.findAllByUserIdOrderByDateDesc(1L)).thenReturn(List.of(history));

        assertThat(service.myHistory(1L)).singleElement()
                .satisfies(r -> {
                    assertThat(r.actionId()).isEqualTo(2L);
                    assertThat(r.actionName()).isEqualTo("Ação 2");
                    assertThat(r.pointsEarned()).isEqualTo(30);
                });
    }
}
