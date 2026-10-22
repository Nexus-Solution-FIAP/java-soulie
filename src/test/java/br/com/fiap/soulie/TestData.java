package br.com.fiap.soulie;

import br.com.fiap.soulie.domain.action.Action;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.user.User;

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
