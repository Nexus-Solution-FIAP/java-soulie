package br.com.fiap.soulie.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

    List<FeedComment> findAllByPost_IdOrderByCreatedAtAsc(Long postId);

    long countByPost_Id(Long postId);
}
