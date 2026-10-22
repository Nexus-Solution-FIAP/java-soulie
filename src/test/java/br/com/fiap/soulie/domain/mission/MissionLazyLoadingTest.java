package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.domain.action.Action;
import br.com.fiap.soulie.domain.action.ActionRepository;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.evidence.EvidenceService;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regressão para carregamento lazy fora de transação.
 *
 * <p>O projeto usa {@code spring.jpa.open-in-view: false}, então a sessão do
 * Hibernate fecha quando o método do repository retorna. Se um método de leitura
 * do service não for {@code @Transactional}, a entidade volta **destacada** e
 * qualquer acesso a associação {@code LAZY} depois disso estoura
 * {@code LazyInitializationException} — 500 na rota inteira.
 *
 * <p>Este teste roda **sem** transação de propósito. Não dá para escrevê-lo com
 * {@code @DataJpaTest}: lá cada teste roda dentro de uma transação, a entidade
 * nunca fica destacada, e o teste passaria sem provar nada — foi exatamente o
 * que aconteceu na primeira tentativa.
 */
@SpringBootTest
@ActiveProfiles("test")
class MissionLazyLoadingTest {

    @Autowired
    private MissionService missionService;

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Test
    @DisplayName("GET /mission/{id}: missionDetails resolve action e avatar fora da transação")
    void missionDetailsResolvesLazyAssociations() {
        var dados = prepararMissao("lazy-mission@soulie.com");

        assertThatCode(() -> {
            var resposta = missionService.missionDetails(dados.missionId(), dados.userId());
            // actionName e points vêm de Mission.action, que é LAZY.
            assertThat(resposta.actionName()).isEqualTo("Reciclar");
            assertThat(resposta.points()).isEqualTo(30);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GET /evidence/mission/{id}: evidencesByMission resolve o dono fora da transação")
    void evidencesByMissionResolvesOwner() {
        var dados = prepararMissao("lazy-evidence@soulie.com");

        // O MissionOwnerValidator navega mission.getAvatar().getUser(), as duas
        // associações LAZY.
        assertThatCode(() -> evidenceService.evidencesByMission(dados.missionId(), dados.userId()))
                .doesNotThrowAnyException();
    }

    private Dados prepararMissao(String email) {
        var user = userRepository.save(User.builder()
                .name("Lazy")
                .email(email)
                .password("hash")
                .points(0)
                .active(true)
                .build());
        var avatar = avatarRepository.save(Avatar.builder()
                .name("Soulie")
                .level(0)
                .xp(0)
                .humor(Humor.NEUTRAL)
                .plantType(PlantType.IPE_AMARELO)
                .flowerType(FlowerType.GIRASSOL)
                .user(user)
                .build());
        var action = actionRepository.save(Action.builder()
                .name("Reciclar")
                .description("desc")
                .points(30)
                .build());
        var mission = missionRepository.save(
                new Mission(null, action, avatar, "missão", MissionStatus.ACTIVE));
        return new Dados(mission.getId(), user.getId());
    }

    private record Dados(Long missionId, Long userId) {
    }
}
