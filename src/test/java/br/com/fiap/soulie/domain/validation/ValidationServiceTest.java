package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private ValidationRepository validationRepository;

    @InjectMocks
    private ValidationService service;

    @Test
    void createPendingSavesValidationAsPending() {
        when(validationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var evidence = new Evidence();

        var validation = service.createPending(evidence);

        assertThat(validation.getResultValidation()).isEqualTo(ResultValidation.PENDING);
        assertThat(validation.getEvidence()).isSameAs(evidence);
    }

    @Test
    void resultOfReturnsWhatTheAiWrote() {
        when(validationRepository.findByEvidenceId(1L))
                .thenReturn(Optional.of(new Validation(1L, null, ResultValidation.REJECTED)));

        assertThat(service.resultOf(1L)).isEqualTo(ResultValidation.REJECTED);
    }

    @Test
    void resultOfWithoutValidationIsPending() {
        when(validationRepository.findByEvidenceId(1L)).thenReturn(Optional.empty());

        assertThat(service.resultOf(1L)).isEqualTo(ResultValidation.PENDING);
    }
}
