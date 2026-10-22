package br.com.fiap.soulie.domain.social;

import br.com.fiap.soulie.domain.community.CommunityApiService;
import br.com.fiap.soulie.domain.community.InteractionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.push.ExpoPushService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O caminho do <b>atacante</b>, não o do usuário.
 *
 * <p>Os testes de integração da Fase 5 passaram e não pegaram nenhum dos quatro
 * achados da auditoria, porque provavam que a funcionalidade funciona — não que
 * ela é segura. Estes cobrem id de terceiro, parâmetro fora do esperado e
 * recurso de conta desativada.
 */
@SpringBootTest
@ActiveProfiles("test")
class FollowSecurityTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private FollowService followService;
    @Autowired private CommunityApiService communityApiService;
    @Autowired private UserRepository userRepository;
    @Autowired private InteractionRepository interactionRepository;

    @MockitoBean private ExpoPushService expoPushService;

    private User novoUsuario(String nome) {
        int n = SEQ.incrementAndGet();
        return userRepository.save(User.builder()
                .name(nome)
                .email("sec" + n + "@soulie.com")
                .handle("@sec" + n)
                .password("hash")
                .points(10 * n)
                .active(true)
                .allowNudges(true)
                .notificationsEnabled(true)
                .showFullName(true)
                .build());
    }

    @Test
    @DisplayName("DELETE /follow de um estranho NÃO pode devolver o perfil dele")
    void deixarDeSeguirEstranhoNaoVazaPerfil() {
        var atacante = novoUsuario("Atacante");
        var vitima = novoUsuario("Vitima");

        // Não existe follow nenhum entre os dois. A rota devolvia 200 com nome,
        // handle e pontos — varredura da base inteira por id sequencial.
        var resposta = followService.deixarDeSeguir(atacante, vitima.getId());

        assertThat(resposta.user())
                .as("perfil de quem você nunca seguiu não pode voltar na resposta")
                .isNull();
    }

    @Test
    @DisplayName("DELETE /follow nunca confirma a existência de conta desativada")
    void deixarDeSeguirContaDesativadaNaoConfirmaExistencia() {
        var atacante = novoUsuario("Atacante");
        var desativada = novoUsuario("Removida");
        desativada.deleteUser();
        userRepository.save(desativada);

        // Caso 1: nunca seguiu. Resposta idêntica à de qualquer id inexistente.
        assertThat(followService.deixarDeSeguir(atacante, desativada.getId()).user()).isNull();

        // Caso 2: seguia antes de a pessoa desativar. O follow é removido, mas o
        // perfil não volta: ela pediu exclusão, e o follow antigo não é título
        // para continuar lendo o perfil dela.
        var outro = novoUsuario("Outro");
        followService.seguir(atacante, outro.getId());
        outro.deleteUser();
        userRepository.save(outro);

        assertThat(followService.deixarDeSeguir(atacante, outro.getId()).user()).isNull();
    }

    @Test
    @DisplayName("Cutucada para amigo que desativou a conta é recusada e não grava nada")
    void cutucadaParaAmigoDesativadoEhRecusada() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");
        followService.seguir(ana, bia.getId());
        followService.seguir(bia, ana.getId());

        // B desativa a conta. O follows não é apagado, então a amizade
        // "derivada" sobrevivia à exclusão e a cutucada continuava passando.
        bia.deleteUser();
        userRepository.save(bia);

        long antes = interactionRepository.count();

        assertThatThrownBy(() -> communityApiService.nudge(ana, bia.getId()))
                .isInstanceOf(RuntimeException.class);

        assertThat(interactionRepository.count())
                .as("nenhuma linha deve ser gravada para uma conta excluída")
                .isEqualTo(antes);
    }

    @Test
    @DisplayName("sort por campo do User associado é ignorado")
    void sortAninhadoEhIgnorado() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");
        followService.seguir(ana, bia.getId());

        // `sort=followee.email` permitia extrair o e-mail de um alvo por busca
        // binária, usando uma conta sonda. É a mesma falha do GET /user?sort=email
        // que a Fase 1b fechou — e aqui é pior, porque a entidade paginada é
        // Follow e o Spring Data resolve caminho aninhado.
        assertThatCode(() -> followService.seguindo(
                ana, PageRequest.of(0, 20, Sort.by("followee.email"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sort por campo inexistente não pode virar 500")
    void sortInexistenteNaoEstoura() {
        var ana = novoUsuario("Ana");

        // PropertyReferenceException caía no catch-all do ErrorHandler → 500 com
        // stack por requisição, o afogamento de log que o próprio ErrorHandler
        // diz ter corrigido.
        assertThatCode(() -> followService.seguidores(
                ana, PageRequest.of(0, 20, Sort.by("naoExiste"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A notificação de cutucada usa o nome público, não o real")
    void notificacaoUsaNomePublico() {
        var ana = novoUsuario("Ana Carolina Silva");
        // Ana escolheu esconder o nome real.
        ana.updateSettings(null, null, null, false);
        userRepository.save(ana);

        var bia = novoUsuario("Bia");
        followService.seguir(ana, bia.getId());
        followService.seguir(bia, ana.getId());

        communityApiService.nudge(ana, bia.getId());

        assertThat(communityApiService.notifications(bia))
                .allSatisfy(n -> assertThat(n.body())
                        .as("quem escondeu o nome não deve tê-lo exposto na notificação")
                        .doesNotContain("Ana Carolina Silva"));
    }
}
