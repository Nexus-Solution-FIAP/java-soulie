package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.ai.ImageValidationClient;
import br.com.fiap.soulie.infra.ai.ImageValidationContract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * O processor no perfil dev roda no {@code AFTER_COMMIT} da gravação da
 * evidência. Sem transação nova ali, o {@code UPDATE} do veredito estoura
 * {@code No active transaction} e a foto nunca sai de PENDING.
 */
@SpringBootTest
@ActiveProfiles("test")
class MissionValidateAfterCommitTest {

    @Autowired
    private MissionApiService missionApiService;

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private TransactionTemplate tx;

    @MockitoBean
    private ImageValidationClient validationClient;

    @Test
    @DisplayName("AFTER_COMMIT: a aprovação da IA grava o veredito na mesma requisição")
    void aprovacaoNoAfterCommitNaoFicaPendente() {
        var dados = tx.execute(status -> {
            var user = userRepository.save(User.builder()
                    .name("AfterCommit")
                    .email("after-commit-" + UUID.randomUUID() + "@soulie.com")
                    .password("hash")
                    .points(0)
                    .active(true)
                    .build());
            avatarRepository.save(Avatar.builder()
                    .name("Soulie").level(0).xp(0)
                    .humor(Humor.NEUTRAL).plantType(PlantType.IPE_AMARELO)
                    .flowerType(FlowerType.GIRASSOL).user(user).build());
            var mission = dailyMissionRepository.save(novaMissao());
            return new Dados(user, mission.getId());
        });

        when(validationClient.validate(anyLong(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new ImageValidationContract.Result(
                        "APPROVED", List.of("Muda reconhecida"), 0.91, "Comprova", "teste")));

        var photo = new MockMultipartFile("photo", "muda.jpg", "image/jpeg", jpegMinimo());
        var resposta = missionApiService.validate(dados.user(), dados.missionId(), photo);

        assertThat(resposta.status()).isEqualTo("APPROVED");
        assertThat(resposta.success()).isTrue();
        assertThat(resposta.xpGained()).isEqualTo(50);
        assertThat(resposta.validationId()).isNotNull();
    }

    @Test
    @DisplayName("AFTER_COMMIT: reprovação da IA devolve REJECTED com reason e sem XP")
    void reprovacaoNoAfterCommitDevolveReasonSemXp() {
        var dados = tx.execute(status -> {
            var user = userRepository.save(User.builder()
                    .name("AfterReject")
                    .email("after-reject-" + UUID.randomUUID() + "@soulie.com")
                    .password("hash")
                    .points(0)
                    .active(true)
                    .build());
            avatarRepository.save(Avatar.builder()
                    .name("Soulie").level(0).xp(0)
                    .humor(Humor.NEUTRAL).plantType(PlantType.IPE_AMARELO)
                    .flowerType(FlowerType.GIRASSOL).user(user).build());
            var mission = dailyMissionRepository.save(novaMissao());
            return new Dados(user, mission.getId());
        });

        when(validationClient.validate(anyLong(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new ImageValidationContract.Result(
                        "REJECTED",
                        List.of("Material reciclável separado reconhecido"),
                        0.21,
                        "Não reconhecemos coleta seletiva nesta foto.",
                        "teste")));

        var photo = new MockMultipartFile("photo", "laranja.jpg", "image/jpeg", jpegMinimo());
        var resposta = missionApiService.validate(dados.user(), dados.missionId(), photo);

        assertThat(resposta.status()).isEqualTo("REJECTED");
        assertThat(resposta.success()).isFalse();
        assertThat(resposta.xpGained()).isZero();
        assertThat(resposta.reason()).isEqualTo("Não reconhecemos coleta seletiva nesta foto.");
        assertThat(resposta.validationId()).isNotNull();
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
