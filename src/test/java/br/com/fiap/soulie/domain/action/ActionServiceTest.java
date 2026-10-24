package br.com.fiap.soulie.domain.action;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.action.dto.ActionRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

    @Mock
    private ActionRepository actionRepository;

    @InjectMocks
    private ActionService service;

    @Test
    void createActionSavesRequestData() {
        var response = service.createAction(new ActionRequest("Meditar", "10 minutos", 20));

        var captor = ArgumentCaptor.forClass(Action.class);
        verify(actionRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Meditar");
        assertThat(captor.getValue().getPoints()).isEqualTo(20);
        assertThat(response.description()).isEqualTo("10 minutos");
    }

    @Test
    void actionDetails() {
        when(actionRepository.findById(1L)).thenReturn(Optional.of(TestData.action(1L, 15)));

        assertThat(service.actionDetails(1L).points()).isEqualTo(15);
    }

    @Test
    void actionDetailsNotFound() {
        when(actionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actionDetails(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Ação não encontrada");
    }
}
