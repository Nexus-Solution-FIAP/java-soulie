package br.com.fiap.soulie;

import br.com.fiap.soulie.domain.action.Action;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.garden.Garden;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.reward.RewardCatalog;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.validation.Validation;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

// Base dos testes de repositório: H2 em memória (perfil "test") e helpers que gravam dados de verdade.
// Cada teste roda numa transação que sofre rollback no final, então um teste não suja o outro.
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class RepositoryTestBase {

    @Autowired
    protected TestEntityManager em;

    protected User persistUser(String email, boolean active) {
        return em.persist(User.builder()
                .name(email)
                .email(email)
                .password("hash")
                .points(0)
                .active(active)
                .build());
    }

    protected Avatar persistAvatar(User user) {
        // No java-soulie o campo é `xp` (XP TOTAL) e o nível é derivado por
        // SoulieLevel.levelOf(xp) — com xp 0 o nível é 0, não 1. No
        // soulie-soul-up era `experience` (XP dentro do nível) e o avatar
        // começava no nível 1.
        return em.persist(Avatar.builder()
                .name("Soulie de " + user.getName())
                .level(0)
                .xp(0)
                .humor(Humor.NEUTRAL)
                .plantType(PlantType.IPE_AMARELO)
                .flowerType(FlowerType.GIRASSOL)
                .user(user)
                .build());
    }

    protected Action persistAction(String name, int points) {
        return em.persist(Action.builder().name(name).description("desc").points(points).build());
    }

    protected Mission persistMission(Avatar avatar, Action action, MissionStatus status) {
        return em.persist(new Mission(null, action, avatar, "missão", status));
    }

    protected Evidence persistEvidence(Mission mission, User user) {
        return em.persist(new Evidence(null, user, mission, "https://img/foto.png"));
    }

    protected Validation persistValidation(Evidence evidence, ResultValidation result) {
        return em.persist(new Validation(null, evidence, result));
    }

    protected RewardCatalog persistRewardCatalog(String benefits, int cost) {
        return em.persist(RewardCatalog.builder()
                .benefits(benefits)
                .cost(cost)
                .active(true)
                .build());
    }

    protected Garden persistGarden(String name, User... users) {
        var garden = Garden.builder()
                .name(name)
                .inviteCode(proximoInviteCode())
                .build();
        for (User user : users) {
            garden.addUserToGarden(user);
        }
        return em.persist(garden);
    }

    // Garden.inviteCode é NOT NULL UNIQUE com no máximo 12 caracteres.
    private static int sequenciaDeConvite = 0;

    private static String proximoInviteCode() {
        return "TEST%06d".formatted(++sequenciaDeConvite);
    }

    // Grava tudo e limpa o cache do Hibernate, forçando as consultas a irem ao banco
    protected void flushAndClear() {
        em.flush();
        em.clear();
    }
}
