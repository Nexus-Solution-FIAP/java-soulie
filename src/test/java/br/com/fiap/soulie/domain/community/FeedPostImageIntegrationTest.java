package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.api.dto.CreateFeedRequest;
import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.evidence.EvidenceRepository;
import br.com.fiap.soulie.domain.garden.GardenService;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.domain.validation.Validation;
import br.com.fiap.soulie.domain.validation.ValidationRepository;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import br.com.fiap.soulie.infra.push.ExpoPushService;
import br.com.fiap.soulie.infra.storage.StoredImage;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class FeedPostImageIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private CommunityApiService communityApiService;
    @Autowired private GardenService gardenService;
    @Autowired private UserRepository userRepository;
    @Autowired private DailyMissionRepository dailyMissionRepository;
    @Autowired private EvidenceRepository evidenceRepository;
    @Autowired private ValidationRepository validationRepository;

    @MockitoBean private ExpoPushService expoPushService;

    @Test
    @DisplayName("validationId aprovado cola a URL HTTP da evidência e ignora blob:")
    void publishUsaUrlDaEvidenciaAprovada() {
        var ana = novoUsuario("Ana");
        gardenService.openMine(ana);
        var validation = persistirValidacao(ana, ResultValidation.APPROVED, "https://cdn.example/missao.jpg");

        var post = communityApiService.publish(ana, new CreateFeedRequest(
                "Completei a missão!",
                "blob:http://localhost/foto",
                "Ir de bike",
                40,
                validation.getId()
        ));

        assertThat(post.imageUri()).isEqualTo("https://cdn.example/missao.jpg");
        assertThat(communityApiService.feed(ana).getFirst().imageUri())
                .isEqualTo("https://cdn.example/missao.jpg");
    }

    @Test
    @DisplayName("blob: e file: no imageUri não entram no feed")
    void publishDescartaUriLocal() {
        var ana = novoUsuario("Ana");
        gardenService.openMine(ana);

        var post = communityApiService.publish(ana, new CreateFeedRequest(
                "Sem foto pública",
                "file:///data/user/0/cache/foto.jpg",
                null,
                null,
                null
        ));

        assertThat(post.imageUri()).isNull();
    }

    @Test
    @DisplayName("Validação de outra pessoa não vira foto no meu post")
    void validationDeOutroUsuarioERecusada() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");
        gardenService.openMine(ana);
        var deBia = persistirValidacao(bia, ResultValidation.APPROVED, "https://cdn.example/bia.jpg");

        assertThatThrownBy(() -> communityApiService.publish(ana, new CreateFeedRequest(
                "Não é minha foto", null, null, null, deBia.getId()
        ))).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Validação pendente não publica foto")
    void validationPendenteERecusada() {
        var ana = novoUsuario("Ana");
        gardenService.openMine(ana);
        var pendente = persistirValidacao(ana, ResultValidation.PENDING, "https://cdn.example/pendente.jpg");

        assertThatThrownBy(() -> communityApiService.publish(ana, new CreateFeedRequest(
                "Ainda analisando", null, null, null, pendente.getId()
        ))).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("validationId inexistente responde 404 de negócio")
    void validationDesconhecida() {
        var ana = novoUsuario("Ana");
        gardenService.openMine(ana);

        assertThatThrownBy(() -> communityApiService.publish(ana, new CreateFeedRequest(
                "Foto fantasma", null, null, null, 9_999_999L
        ))).isInstanceOf(EntityNotFoundException.class);
    }

    private Validation persistirValidacao(User user, ResultValidation resultado, String imageUrl) {
        var mission = novaMissao();
        var evidence = evidenceRepository.save(Evidence.doAppParaMissaoDiaria(
                user,
                mission,
                new StoredImage("evidences/" + UUID.randomUUID() + ".jpg", imageUrl, "image/jpeg", 12)
        ));
        return validationRepository.save(Validation.builder()
                .evidence(evidence)
                .resultValidation(resultado)
                .requestedAt(LocalDateTime.now())
                .build());
    }

    private DailyMission novaMissao() {
        var mission = new DailyMission();
        ReflectionTestUtils.setField(mission, "emoji", "🌱");
        ReflectionTestUtils.setField(mission, "name", "Missão feed " + UUID.randomUUID());
        ReflectionTestUtils.setField(mission, "tag", "Natureza");
        ReflectionTestUtils.setField(mission, "description", "Descrição");
        ReflectionTestUtils.setField(mission, "xp", 40);
        ReflectionTestUtils.setField(mission, "co2SavedKg", 0.4);
        ReflectionTestUtils.setField(mission, "waterSavedLiters", 0.0);
        return dailyMissionRepository.save(mission);
    }

    private User novoUsuario(String nome) {
        int n = SEQ.incrementAndGet();
        return userRepository.save(User.builder()
                .name(nome)
                .email("feedimg" + n + "@soulie.com")
                .handle("@feedimg" + n)
                .password("hash")
                .points(0)
                .active(true)
                .showFullName(true)
                .build());
    }
}
