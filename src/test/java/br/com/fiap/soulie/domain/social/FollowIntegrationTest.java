package br.com.fiap.soulie.domain.social;

import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integração do grafo social, pelo caminho real e <b>fora de transação de
 * teste</b>.
 *
 * <p>Isto não é zelo: duas vezes neste projeto uma entrega foi declarada pronta
 * com a suíte verde e estava quebrada em toda chamada — as rotas de missão e
 * evidência, e depois o pipeline de validação inteiro. As duas eram a mesma
 * falha: método não transacional devolvendo entidade destacada, e
 * {@code LazyInitializationException} no primeiro acesso a uma associação
 * {@code LAZY}.
 *
 * <p>E {@code @DataJpaTest} <b>esconde</b> esse bug, porque envolve cada teste
 * numa transação. Por isso aqui é {@code @SpringBootTest} sem transação: é o
 * único arranjo em que o problema aparece.
 */
@SpringBootTest
@ActiveProfiles("test")
class FollowIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private FollowService followService;
    @Autowired private FollowRepository followRepository;
    @Autowired private UserRepository userRepository;

    private User novoUsuario(String nome) {
        int n = SEQ.incrementAndGet();
        return userRepository.save(User.builder()
                .name(nome)
                .email("social" + n + "@soulie.com")
                .handle("@social" + n)
                .password("hash")
                .points(10 * n)
                .active(true)
                .showFullName(true)
                .build());
    }

    @Test
    @DisplayName("Percorrer o grafo fora de transação não estoura lazy")
    void percorrerGrafoForaDeTransacao() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");

        followService.seguir(ana, bia.getId());

        // Este é o teste que teria pegado os dois bugs anteriores: cada uma
        // destas chamadas toca Follow.follower / Follow.followee, que são LAZY.
        assertThatCode(() -> {
            followService.seguidores(bia, PageRequest.of(0, 20)).getContent()
                    .forEach(s -> assertThat(s.name()).isNotBlank());
            followService.seguindo(ana, PageRequest.of(0, 20)).getContent()
                    .forEach(s -> assertThat(s.name()).isNotBlank());
            followService.amigos(ana, PageRequest.of(0, 20)).forEach(s -> assertThat(s.name()).isNotBlank());
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Ciclo completo: seguir unilateral, virar amigos, desfazer")
    void cicloCompletoDeAmizade() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");

        // A segue B: ninguém é amigo de ninguém ainda.
        var primeiro = followService.seguir(ana, bia.getId());
        assertThat(primeiro.following()).isTrue();
        assertThat(primeiro.friendsNow()).isFalse();
        assertThat(followService.amigos(ana, PageRequest.of(0, 20))).isEmpty();
        assertThat(followService.amigos(bia, PageRequest.of(0, 20))).isEmpty();

        // B segue A de volta: agora são amigos, nos dois sentidos.
        var segundo = followService.seguir(bia, ana.getId());
        assertThat(segundo.friendsNow()).isTrue();
        assertThat(followService.amigos(ana, PageRequest.of(0, 20)).getContent()).extracting("id").containsExactly(bia.getId());
        assertThat(followService.amigos(bia, PageRequest.of(0, 20)).getContent()).extracting("id").containsExactly(ana.getId());

        // A deixa de seguir: a amizade se desfaz por consequência, porque ela é
        // derivada e não armazenada.
        followService.deixarDeSeguir(ana, bia.getId());
        assertThat(followService.amigos(ana, PageRequest.of(0, 20))).isEmpty();
        assertThat(followService.amigos(bia, PageRequest.of(0, 20))).isEmpty();
        // Mas B continua seguindo A.
        assertThat(followService.seguindo(bia, PageRequest.of(0, 20))).hasSize(1);
    }

    @Test
    @DisplayName("Seguir duas vezes não duplica a linha")
    void seguirDuasVezesEhIdempotente() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");

        followService.seguir(ana, bia.getId());
        followService.seguir(ana, bia.getId());

        assertThat(followRepository.countByFollower_Id(ana.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Seguir a si mesmo é recusado pelo validator")
    void selfFollowRecusadoPeloValidator() {
        var ana = novoUsuario("Ana");

        assertThatThrownBy(() -> followService.seguir(ana, ana.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("si mesmo");
    }

    @Test
    @DisplayName("Seguir a si mesmo é recusado também pelo CHECK do banco")
    void selfFollowRecusadoPeloBanco() {
        var ana = novoUsuario("Ana");

        // Contorna o service de propósito: prova que a regra NÃO depende do
        // código estar certo. Validação só na aplicação é contornável por
        // corrida ou por um caminho novo que esqueça de chamar o validator.
        assertThatThrownBy(() -> followRepository.saveAndFlush(new Follow(ana, ana)))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Conta desativada não pode ser seguida")
    void naoSegueContaDesativada() {
        var ana = novoUsuario("Ana");
        var desativada = novoUsuario("Removida");
        desativada.deleteUser();
        userRepository.save(desativada);

        // 404 e não 403: a existência de uma conta desativada não precisa ser
        // confirmada a quem tentou segui-la.
        assertThatThrownBy(() -> followService.seguir(ana, desativada.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Busca por handle exato acha; por nome parcial, não existe")
    void buscaPorHandleExato() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");

        var achada = followService.buscarPorHandle(ana, bia.getHandle());
        assertThat(achada.id()).isEqualTo(bia.getId());

        // Funciona com e sem o arroba, para o app não precisar normalizar.
        assertThat(followService.buscarPorHandle(ana, bia.getHandle().substring(1)).id())
                .isEqualTo(bia.getId());

        // Pedaço do nome não acha nada: não há busca parcial, de propósito.
        assertThatThrownBy(() -> followService.buscarPorHandle(ana, "Bi"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Conta desativada não aparece na busca")
    void buscaIgnoraContaDesativada() {
        var ana = novoUsuario("Ana");
        var desativada = novoUsuario("Removida");
        desativada.deleteUser();
        userRepository.save(desativada);

        assertThatThrownBy(() -> followService.buscarPorHandle(ana, desativada.getHandle()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
