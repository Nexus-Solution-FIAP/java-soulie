package br.com.fiap.soulie.domain.garden;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GardenRepository extends JpaRepository<Garden, Long> {

    @EntityGraph(attributePaths = "users")
    List<Garden> findAllByUsersId(Long userId);

    @EntityGraph(attributePaths = "users")
    List<Garden> findAllByUsersIdOrderByIdAsc(Long userId);

    @EntityGraph(attributePaths = "users")
    Optional<Garden> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    @EntityGraph(attributePaths = "users")
    Optional<Garden> findByIdAndUsersId(Long gardenId, Long userId);

    Boolean existsByIdAndUsersId(Long gardenId, Long userId);
}
