package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.domain.social.FollowService;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.push.ExpoPushService;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A cutucada passou a aceitar <b>mesmo jardim OU amizade mútua</b>.
 *
 * <p>Teste de integração pelo caminho real, fora de transação: ele atravessa
 * {@code User}, {@code Follow}, {@code Interaction} e {@code UserNotification},
 * que é exatamente onde a associação {@code LAZY} apareceria se algum service
 * de leitura não fosse transacional.
 */
@SpringBootTest
@ActiveProfiles("test")
class NudgeFriendshipIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private CommunityApiService communityApiService;
    @Autowired private FollowService followService;
    @Autowired private UserRepository userRepository;

    // O push é uma chamada HTTP de saída; nos testes vira mock.
    @MockitoBean private ExpoPushService expoPushService;

    private User novoUsuario(String nome) {
        int n = SEQ.incrementAndGet();
        return userRepository.save(User.builder()
                .name(nome)
                .email("nudge" + n + "@soulie.com")
                .handle("@nudge" + n)
                .password("hash")
                .points(0)
                .active(true)
                .allowNudges(true)
                .notificationsEnabled(true)
                .showFullName(true)
                .build());
    }

    @Test
    @DisplayName("Amigos mútuos se cutucam mesmo sem dividir o jardim")
    void amigosMutuosSeCutucamSemJardim() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");

        // Nenhum dos dois está em jardim nenhum: antes desta fase, isto era
        // impossível.
        followService.seguir(ana, bia.getId());
        followService.seguir(bia, ana.getId());

        assertThatCode(() -> communityApiService.nudge(ana, bia.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Seguir sozinho não basta: precisa ser mútuo")
    void seguirUnilateralNaoLiberaCutucada() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");

        // A segue B, mas B não segue A de volta.
        followService.seguir(ana, bia.getId());

        assertThatThrownBy(() -> communityApiService.nudge(ana, bia.getId()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Estranho continua sem poder cutucar")
    void estranhoNaoCutuca() {
        var ana = novoUsuario("Ana");
        var estranho = novoUsuario("Estranho");

        assertThatThrownBy(() -> communityApiService.nudge(ana, estranho.getId()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("A cota de 3 por dia continua valendo pelo caminho novo")
    void cotaDiariaValeParaAmigos() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");
        followService.seguir(ana, bia.getId());
        followService.seguir(bia, ana.getId());

        // A cota existia só para o caminho do jardim; precisa valer igual aqui,
        // senão a amizade vira um jeito de burlar o limite.
        var primeira = communityApiService.nudge(ana, bia.getId());
        assertThat(primeira.remaining()).isEqualTo(2);
        communityApiService.nudge(ana, bia.getId());
        communityApiService.nudge(ana, bia.getId());

        assertThatThrownBy(() -> communityApiService.nudge(ana, bia.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("3 vezes hoje");
    }

    @Test
    @DisplayName("Quem desligou cutucadas não recebe, nem de amigo")
    void respeitaPreferenciaDeNaoReceber() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");
        bia.updateSettings(null, null, false, null);
        userRepository.save(bia);

        followService.seguir(ana, bia.getId());
        followService.seguir(bia, ana.getId());

        assertThatThrownBy(() -> communityApiService.nudge(ana, bia.getId()))
                .isInstanceOf(ValidationException.class);
    }
}
