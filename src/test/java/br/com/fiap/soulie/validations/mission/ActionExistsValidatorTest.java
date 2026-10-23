package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.action.ActionRepository;
import br.com.fiap.soulie.domain.mission.dto.MissionRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionExistsValidatorTest {

    @Mock
    private ActionRepository actionRepository;

    @InjectMocks
    private ActionExistsValidator validator;

    @Test
    void passesWhenActionExists() {
        when(actionRepository.existsById(5L)).thenReturn(true);

        assertThatCode(() -> validator.validate(new MissionRequest(5L, "desc"), 1L)).doesNotThrowAnyException();
    }

    @Test
    void failsWhenActionDoesNotExist() {
        when(actionRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> validator.validate(new MissionRequest(5L, "desc"), 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Ação não encontrada");
    }
}
