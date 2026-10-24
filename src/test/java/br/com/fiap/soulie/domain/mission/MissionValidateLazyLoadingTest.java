package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@code POST /v1/missions/{id}/validate} fora de transação.
 *
 * <p>{@code MissionApiService.validate} não é {@code @Transactional}: o upload
 * e a espera da IA não podem reter conexão. No fim ele chama
 * {@code statusDaValidacao} no mesmo bean, então a anotação transacional dali
 * não vale. Sem {@code join fetch} na evidência, o primeiro
 * {@code evidence.getUser()} estoura {@code LazyInitializationException} — 500
 * em todo envio de foto. {@code @DataJpaTest} esconderia exatamente isso.
 */
@SpringBootTest
@ActiveProfiles("test")
class MissionValidateLazyLoadingTest {

    @Autowired
    private MissionApiService missionApiService;

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate tx;

    @Test
    @DisplayName("POST /v1/missions/{id}/validate: statusDaValidacao resolve evidence.user fora da transação")
    void validateNaoEstouraLazyNaResposta() {
        var dados = tx.execute(status -> {
            var user = userRepository.save(User.builder()
                    .name("Valida")
                    .email("validate-lazy-" + UUID.randomUUID() + "@soulie.com")
                    .password("hash")
                    .points(0)
                    .active(true)
                    .build());
            var mission = dailyMissionRepository.save(novaMissao());
            return new Dados(user, mission.getId());
        });

        var photo = new MockMultipartFile(
                "photo", "muda.jpg", "image/jpeg", jpegMinimo());

        assertThatCode(() -> {
            var resposta = missionApiService.validate(dados.user(), dados.missionId(), photo);
            assertThat(resposta.missionId()).isEqualTo(dados.missionId());
            assertThat(resposta.validationId()).isNotNull();
            assertThat(resposta.status()).isEqualTo("PENDING");
        }).doesNotThrowAnyException();
    }

    private DailyMission novaMissao() {
        var mission = new DailyMission();
        ReflectionTestUtils.setField(mission, "emoji", "🌱");
        ReflectionTestUtils.setField(mission, "name", "Plantar uma Muda " + UUID.randomUUID());
        ReflectionTestUtils.setField(mission, "tag", "Natureza");
        ReflectionTestUtils.setField(mission, "description", "Plante uma muda");
        ReflectionTestUtils.setField(mission, "xp", 50);
        ReflectionTestUtils.setField(mission, "co2SavedKg", 0.5);
        ReflectionTestUtils.setField(mission, "waterSavedLiters", 0.0);
        return mission;
    }

    private static void marcadorComTamanho(ByteArrayOutputStream saida, int marcador, byte[] dados) {
        saida.write(0xFF);
        saida.write(marcador);
        int tamanho = dados.length + 2;
        saida.write((tamanho >>> 8) & 0xFF);
        saida.write(tamanho & 0xFF);
        saida.writeBytes(dados);
    }

    /** JPEG mínimo que passa pelo ImageSanitizer (SOI, APP0, SOS, EOI). */
    private static byte[] jpegMinimo() {
        var saida = new ByteArrayOutputStream();
        saida.write(0xFF);
        saida.write(0xD8);
        marcadorComTamanho(saida, 0xE0, "JFIF\0".getBytes(StandardCharsets.US_ASCII));
        marcadorComTamanho(saida, 0xDA, new byte[]{0x01, 0x02});
        saida.writeBytes(new byte[]{0x11, 0x22, 0x33});
        saida.write(0xFF);
        saida.write(0xD9);
        return saida.toByteArray();
    }

    private record Dados(User user, Long missionId) {
    }
}
