package br.com.fiap.soulie.domain.reward;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    @EntityGraph(attributePaths = "user")
    List<Reward> findAllByUserId(Long userId);
}
