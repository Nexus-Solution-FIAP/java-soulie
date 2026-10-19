package br.com.fiap.soulie.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    long countBySender_IdAndReceiver_IdAndKindAndCreatedAtGreaterThanEqual(
            Long senderId,
            Long receiverId,
            String kind,
            LocalDateTime createdAt
    );
}
