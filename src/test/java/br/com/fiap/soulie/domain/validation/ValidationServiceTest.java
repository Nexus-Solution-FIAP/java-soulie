package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import br.com.fiap.soulie.infra.messaging.ValidationRequestedEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private ValidationRepository validationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ValidationService service;

    @Test
    void createPendingSavesValidationAsPending() {
        when(validationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var evidence = new Evidence();

        var validation = service.createPending(evidence);

        assertThat(validation.getResultValidation()).isEqualTo(ResultValidation.PENDING);
        assertThat(validation.getEvidence()).isSameAs(evidence);

        // O pedido de analise e disparado como evento, nao publicado direto na
        // fila: quem fala com o RabbitMQ so age depois do commit, para o worker
        // nao consumir uma validacao que ainda nao existe no banco.
        verify(eventPublisher).publishEvent(any(ValidationRequestedEvent.class));
    }

    @Test
    void resultOfReturnsWhatTheAiWrote() {
        when(validationRepository.findByEvidenceId(1L))
                .thenReturn(Optional.of(Validation.builder().id(1L).resultValidation(ResultValidation.REJECTED).build()));

        assertThat(service.resultOf(1L)).isEqualTo(ResultValidation.REJECTED);
    }

    @Test
    void resultOfWithoutValidationIsPending() {
        when(validationRepository.findByEvidenceId(1L)).thenReturn(Optional.empty());

        assertThat(service.resultOf(1L)).isEqualTo(ResultValidation.PENDING);
    }
}
