package br.com.fiap.soulie.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {
    Optional<FeedLike> findByPost_IdAndUser_Id(Long postId, Long userId);

    long countByPost_Id(Long postId);
}
