package br.com.fiap.soulie.domain.mission;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @EntityGraph(attributePaths = "action")
    List<Mission> findAllByAvatarUserId(Long userId);
}
