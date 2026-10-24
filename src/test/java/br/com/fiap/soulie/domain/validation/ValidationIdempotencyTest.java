package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import br.com.fiap.soulie.infra.ai.ImageValidationContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reentrega de mensagem do RabbitMQ é comportamento normal — acontece em
 * reconexão, em ack perdido, em redeploy no meio do processamento.
 *
 * <p>Se a resolução não fosse idempotente, cada reentrega creditaria o XP de
 * novo. É o mesmo problema do duplo gasto no resgate de recompensa, e a solução
 * é a mesma: deixar o banco decidir a transição, numa instrução só.
 */
@ExtendWith(MockitoExtension.class)
class ValidationIdempotencyTest {

    @Mock private ValidationRepository validationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ValidationService service;

    private Validation pendente;

    @BeforeEach
    void setUp() {
        pendente = Validation.builder()
                .id(7L)
                .evidence(Evidence.builder().id(1L).build())
                .resultValidation(ResultValidation.PENDING)
                .build();
    }

    private ImageValidationContract.Result aprovado() {
        return new ImageValidationContract.Result(
                "APPROVED", List.of("Bicicleta reconhecida"), 0.93, "A foto comprova", "vision-v2");
    }

    @Test
    @DisplayName("A primeira entrega resolve e devolve a validação")
    void primeiraEntregaResolve() {
        when(validationRepository.resolveIfPending(7L, ResultValidation.APPROVED)).thenReturn(1);
        when(validationRepository.findById(7L)).thenReturn(Optional.of(pendente));
        when(validationRepository.save(any(Validation.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = service.resolve(7L, aprovado());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getResultValidation()).isEqualTo(ResultValidation.APPROVED);
        assertThat(resultado.get().detectionList()).containsExactly("Bicicleta reconhecida");
    }

    @Test
    @DisplayName("A reentrega não resolve de novo: o UPDATE condicional não afeta linha")
    void reentregaNaoResolveDeNovo() {
        // 0 linhas afetadas = outra entrega ja resolveu esta validacao.
        when(validationRepository.resolveIfPending(7L, ResultValidation.APPROVED)).thenReturn(0);

        var resultado = service.resolve(7L, aprovado());

        assertThat(resultado).isEmpty();
        // Nada mais acontece: nem leitura, nem gravacao — e, la em cima, nem XP.
        verify(validationRepository, never()).findById(any());
        verify(validationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Status desconhecido da IA vira FAILED, nunca REJECTED")
    void statusDesconhecidoViraFailed() {
        when(validationRepository.resolveIfPending(eq(7L), eq(ResultValidation.FAILED))).thenReturn(1);
        when(validationRepository.findById(7L)).thenReturn(Optional.of(pendente));
        when(validationRepository.save(any(Validation.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = service.resolve(7L, new ImageValidationContract.Result(
                "ERRO_INTERNO_DA_IA", null, null, null, null));

        // A diferenca importa: REJECTED e "a foto nao comprova"; FAILED e "nao
        // conseguimos analisar". Tratar igual puniria quem fez a acao certa.
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getResultValidation()).isEqualTo(ResultValidation.FAILED);
    }

    @Test
    @DisplayName("Resposta nula da IA também vira FAILED")
    void respostaNulaViraFailed() {
        when(validationRepository.resolveIfPending(eq(7L), eq(ResultValidation.FAILED))).thenReturn(1);
        when(validationRepository.findById(7L)).thenReturn(Optional.of(pendente));
        when(validationRepository.save(any(Validation.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = service.resolve(7L, null);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getResultValidation()).isEqualTo(ResultValidation.FAILED);
    }

    @Test
    @DisplayName("A grafia antiga APROVED do soulie-soul-up continua sendo aceita")
    void aceitaGrafiaAntiga() {
        when(validationRepository.resolveIfPending(eq(7L), eq(ResultValidation.APPROVED))).thenReturn(1);
        when(validationRepository.findById(7L)).thenReturn(Optional.of(pendente));
        when(validationRepository.save(any(Validation.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = service.resolve(7L, new ImageValidationContract.Result(
                "aproved", List.of(), null, null, null));

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getResultValidation()).isEqualTo(ResultValidation.APPROVED);
    }
}
