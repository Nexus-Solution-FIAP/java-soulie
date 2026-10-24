package br.com.fiap.soulie.domain.avatar;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<Avatar> findByUserId(Long userId);

    Boolean existsByUserId(Long id);

    @EntityGraph(attributePaths = "user")
    List<Avatar> findAllByUserGardensId(Long gardenId);

    List<Avatar> findByUserIdIn(Collection<Long> userIds);
}
