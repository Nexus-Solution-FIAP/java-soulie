package br.com.fiap.soulie.validations.community;

import br.com.fiap.soulie.domain.community.FeedPost;

public interface FeedPostDeletionValidator {
    void validate(FeedPost post, Long userId);
}
