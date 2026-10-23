package br.com.fiap.soulie.validations.community;

import br.com.fiap.soulie.domain.community.FeedPost;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Só o autor apaga a publicação. No mesmo jardim, id de terceiro dá 403.
 * Fora do jardim o service responde 404 antes, para não vazar existência.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class FeedPostOwnerValidator implements FeedPostDeletionValidator {

    @Override
    public void validate(FeedPost post, Long userId) {
        if (post.getAuthor() == null || !post.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Você só pode apagar as suas publicações");
        }
    }
}
