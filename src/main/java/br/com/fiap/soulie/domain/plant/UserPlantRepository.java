package br.com.fiap.soulie.domain.plant;

import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPlantRepository extends JpaRepository<UserPlant, Long> {

    List<UserPlant> findByUser_IdOrderByIdAsc(Long userId);

    Optional<UserPlant> findByUser_IdAndPlantType(Long userId, FlowerType plantType);

    long countByUser_IdAndUnlockedTrue(Long userId);

    long countByUser_IdAndUnlockedTrueAndXpGreaterThanEqual(Long userId, int xp);
}
