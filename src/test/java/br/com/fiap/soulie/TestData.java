package br.com.fiap.soulie;

import br.com.fiap.soulie.domain.action.Action;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.reward.RewardCatalog;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.enums.UserRole;

// "Fábrica" de objetos prontos para os testes não repetirem montagem de entidades
public final class TestData {

    private TestData() {
    }

    public static User user(Long id) {
        return User.builder()
                .id(id)
                .name("Usuário " + id)
                .email("user" + id + "@soulie.com")
                .password("Senha@123")
                .points(0)
                .active(true)
                .build();
    }

    /** Usuario com papel ADMIN, para as rotas administrativas. */
    public static User admin(Long id) {
        return User.builder()
                .id(id)
                .name("Admin " + id)
                .email("admin" + id + "@soulie.com")
                .password("Senha@123")
                .points(0)
                .active(true)
                .role(UserRole.ADMIN)
                .build();
    }

    public static User userWithPoints(Long id, int points) {
        return User.builder()
                .id(id)
                .name("Usuário " + id)
                .email("user" + id + "@soulie.com")
                .password("Senha@123")
                .points(points)
                .active(true)
                .build();
    }

    public static Avatar avatar(Long id, User user) {
        return Avatar.builder()
                .id(id)
                .name("Soulie " + id)
                .level(0)
                .xp(0)
                .humor(Humor.NEUTRAL)
                .plantType(PlantType.IPE_AMARELO)
                .flowerType(FlowerType.GIRASSOL)
                .user(user)
                .build();
    }

    public static RewardCatalog rewardCatalog(Long id, String benefits, int cost) {
        return RewardCatalog.builder()
                .id(id)
                .benefits(benefits)
                .cost(cost)
                .active(true)
                .build();
    }

    public static Action action(Long id, int points) {
        return Action.builder()
                .id(id)
                .name("Ação " + id)
                .description("Descrição da ação " + id)
                .points(points)
                .build();
    }

    public static Mission mission(Long id, Avatar avatar, Action action, MissionStatus status) {
        return new Mission(id, action, avatar, "Missão " + id, status);
    }
}
