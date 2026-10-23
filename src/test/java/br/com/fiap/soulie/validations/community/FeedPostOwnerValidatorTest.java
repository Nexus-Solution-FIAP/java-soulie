package br.com.fiap.soulie.validations.community;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.community.FeedPost;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedPostOwnerValidatorTest {

    private final FeedPostOwnerValidator validator = new FeedPostOwnerValidator();

    @Test
    void passaParaOAutor() {
        var autor = TestData.user(1L);
        var post = FeedPost.create(autor, null, "Fui de bike.", null, null, null);
        assertThatCode(() -> validator.validate(post, 1L)).doesNotThrowAnyException();
    }

    @Test
    void recusaOutroUsuario() {
        var autor = TestData.user(1L);
        var post = FeedPost.create(autor, null, "Fui de bike.", null, null, null);
        assertThatThrownBy(() -> validator.validate(post, 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Você só pode apagar as suas publicações");
    }
}
