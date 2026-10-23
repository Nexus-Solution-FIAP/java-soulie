package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.api.dto.CreateCommentRequest;
import br.com.fiap.soulie.api.dto.CreateFeedRequest;
import br.com.fiap.soulie.domain.garden.GardenService;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.push.ExpoPushService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Apagar post pelo caminho real, fora de transação de teste — toca autor e
 * jardim {@code LAZY}, curtidas e comentários.
 */
@SpringBootTest
@ActiveProfiles("test")
class FeedPostDeleteIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private CommunityApiService communityApiService;
    @Autowired private GardenService gardenService;
    @Autowired private UserRepository userRepository;
    @Autowired private FeedPostRepository feedPostRepository;
    @Autowired private FeedLikeRepository feedLikeRepository;
    @Autowired private FeedCommentRepository feedCommentRepository;

    @MockitoBean private ExpoPushService expoPushService;

    private User novoUsuario(String nome) {
        int n = SEQ.incrementAndGet();
        return userRepository.save(User.builder()
                .name(nome)
                .email("feeddel" + n + "@soulie.com")
                .handle("@feeddel" + n)
                .password("hash")
                .points(0)
                .active(true)
                .showFullName(true)
                .build());
    }

    @Test
    @DisplayName("O autor apaga o post, e curtidas e comentários saem junto")
    void autorApagaPostComDependencias() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");
        var garden = gardenService.openMine(ana);
        gardenService.join(bia, garden.inviteCode());

        var post = communityApiService.publish(ana, new CreateFeedRequest("Fui de bike.", null, null, null));
        assertThat(post.mine()).isTrue();
        communityApiService.toggleLike(bia, post.id());
        communityApiService.comment(bia, post.id(), new CreateCommentRequest("Mandou bem"));

        communityApiService.deletePost(ana, post.id());

        assertThat(feedPostRepository.findById(post.id())).isEmpty();
        assertThat(feedLikeRepository.countByPost_Id(post.id())).isZero();
        assertThat(feedCommentRepository.countByPost_Id(post.id())).isZero();
        assertThat(communityApiService.feed(ana)).isEmpty();
    }

    @Test
    @DisplayName("Quem está no jardim, mas não é o autor, não apaga")
    void colegaDoJardimNaoApaga() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");
        var garden = gardenService.openMine(ana);
        gardenService.join(bia, garden.inviteCode());
        var post = communityApiService.publish(ana, new CreateFeedRequest("Plantei uma muda.", null, null, null));

        assertThatThrownBy(() -> communityApiService.deletePost(bia, post.id()))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(feedPostRepository.findById(post.id())).isPresent();
    }

    @Test
    @DisplayName("Post de outro jardim responde como não encontrado")
    void postDeOutroJardimNaoVaza() {
        var ana = novoUsuario("Ana");
        var carla = novoUsuario("Carla");
        gardenService.openMine(ana);
        gardenService.openMine(carla);
        var post = communityApiService.publish(ana, new CreateFeedRequest("Só no meu jardim.", null, null, null));

        assertThatThrownBy(() -> communityApiService.deletePost(carla, post.id()))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(feedPostRepository.findById(post.id())).isPresent();
    }

    @Test
    @DisplayName("Quem publicou ainda apaga depois de sair do jardim")
    void autorApagaDepoisDeSairDoJardim() {
        var ana = novoUsuario("Ana");
        var bia = novoUsuario("Bia");
        var garden = gardenService.openMine(ana);
        gardenService.join(bia, garden.inviteCode());
        var post = communityApiService.publish(ana, new CreateFeedRequest("Vou sair do jardim.", null, null, null));
        gardenService.leave(ana);

        communityApiService.deletePost(ana, post.id());

        assertThat(feedPostRepository.findById(post.id())).isEmpty();
    }
}
