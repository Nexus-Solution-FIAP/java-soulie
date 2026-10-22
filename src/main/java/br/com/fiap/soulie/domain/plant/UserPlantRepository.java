package br.com.fiap.soulie.domain.plant;

import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPlantRepository extends JpaRepository<UserPlant, Long> {

    List<UserPlant> findByUser_IdOrderByIdAsc(Long userId);

    Optional<UserPlant> findByUser_IdAndPlantType(Long userId, FlowerType plantType);

    long countByUser_IdAndUnlockedTrue(Long userId);

    long countByUser_IdAndUnlockedTrueAndXpGreaterThanEqual(Long userId, int xp);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_plants (user_id, plant_type, xp, unlocked)
            VALUES (:userId, :plantType, 0, false)
            ON CONFLICT ON CONSTRAINT uq_user_plants DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("plantType") String plantType);
}
