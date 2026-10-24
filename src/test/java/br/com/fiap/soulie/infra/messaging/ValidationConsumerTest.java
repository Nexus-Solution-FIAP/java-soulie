package br.com.fiap.soulie.infra.messaging;

import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.evidence.EvidenceRepository;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.domain.validation.Validation;
import br.com.fiap.soulie.domain.validation.ValidationRepository;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import br.com.fiap.soulie.infra.ai.ImageValidationClient;
import br.com.fiap.soulie.infra.ai.ImageValidationContract;
import br.com.fiap.soulie.infra.storage.StoredImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * O consumidor roda **fora** de requisição web e **fora** de transação.
 *
 * <p>Com {@code open-in-view: false}, isso significa que qualquer associação
 * {@code LAZY} tocada depois que o repository retornou estoura
 * {@code LazyInitializationException}. Este teste existe porque os 112 testes
 * anteriores passavam sem exercitar o consumidor nenhuma vez — e o pipeline
 * inteiro podia estar quebrado sem nenhum sinal.
 */
@SpringBootTest
@ActiveProfiles("test")
class ValidationConsumerTest {

    @Autowired private ValidationProcessor processor;
    @Autowired private UserRepository userRepository;
    @Autowired private AvatarRepository avatarRepository;
    @Autowired private DailyMissionRepository dailyMissionRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private ValidationRepository validationRepository;

    @MockitoBean private ImageValidationClient validationClient;

    @Test
    @DisplayName("Consumir uma mensagem percorre as associações LAZY sem estourar")
    void consumirResolveAssociacoesLazy() {
        var dados = prepararValidacaoPendente("consumer@soulie.com");

        when(validationClient.validate(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(Optional.of(new ImageValidationContract.Result(
                        "APPROVED", List.of("Bicicleta reconhecida"), 0.9, "Comprova", "teste")));

        assertThatCode(() -> processor.processar(dados.validationId()))
                .doesNotThrowAnyException();

        assertThat(validationRepository.findById(dados.validationId()))
                .get()
                .satisfies(v -> assertThat(v.getResultValidation()).isEqualTo(ResultValidation.APPROVED));
    }

    @Test
    @DisplayName("Aprovação credita o XP da missão ao usuário")
    void aprovacaoCreditaXp() {
        var dados = prepararValidacaoPendente("xp@soulie.com");
        int pontosAntes = userRepository.findById(dados.userId()).orElseThrow().getPoints();

        when(validationClient.validate(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(Optional.of(new ImageValidationContract.Result(
                        "APPROVED", List.of(), 0.9, null, "teste")));

        processor.processar(dados.validationId());

        assertThat(userRepository.findById(dados.userId()).orElseThrow().getPoints())
                .isGreaterThan(pontosAntes);
    }

    @Test
    @DisplayName("Reentrega da mesma mensagem não credita XP de novo")
    void reentregaNaoCreditaDuasVezes() {
        var dados = prepararValidacaoPendente("reentrega@soulie.com");

        when(validationClient.validate(anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(Optional.of(new ImageValidationContract.Result(
                        "APPROVED", List.of(), 0.9, null, "teste")));

        processor.processar(dados.validationId());
        int depoisDaPrimeira = userRepository.findById(dados.userId()).orElseThrow().getPoints();

        // Reentrega: comportamento normal do RabbitMQ.
        processor.processar(dados.validationId());

        assertThat(userRepository.findById(dados.userId()).orElseThrow().getPoints())
                .isEqualTo(depoisDaPrimeira);
    }

    private Dados prepararValidacaoPendente(String email) {
        var user = userRepository.save(User.builder()
                .name("Consumidor").email(email).password("hash")
                .points(0).active(true).build());
        avatarRepository.save(Avatar.builder()
                .name("Soulie").level(0).xp(0)
                .humor(Humor.NEUTRAL).plantType(PlantType.IPE_AMARELO)
                .flowerType(FlowerType.GIRASSOL).user(user).build());
        var mission = dailyMissionRepository.save(novaMissao());
        var evidence = evidenceRepository.save(Evidence.doAppParaMissaoDiaria(
                user, mission, new StoredImage("evidencias/teste.jpg", null, "image/jpeg", 1024)));
        var validation = validationRepository.save(new Validation(evidence));
        return new Dados(validation.getId(), user.getId());
    }

    private DailyMission novaMissao() {
        // DailyMission não tem builder; monta pelo que o catálogo precisa.
        var mission = new DailyMission();
        org.springframework.test.util.ReflectionTestUtils.setField(mission, "emoji", "🚲");
        org.springframework.test.util.ReflectionTestUtils.setField(mission, "name", "Ir de bicicleta");
        org.springframework.test.util.ReflectionTestUtils.setField(mission, "tag", "Mobilidade");
        org.springframework.test.util.ReflectionTestUtils.setField(mission, "description", "Trajeto de bicicleta");
        org.springframework.test.util.ReflectionTestUtils.setField(mission, "xp", 40);
        org.springframework.test.util.ReflectionTestUtils.setField(mission, "co2SavedKg", 0.4);
        org.springframework.test.util.ReflectionTestUtils.setField(mission, "waterSavedLiters", 0.0);
        return mission;
    }

    private record Dados(Long validationId, Long userId) {
    }
}
