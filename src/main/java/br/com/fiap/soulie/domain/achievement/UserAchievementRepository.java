package br.com.fiap.soulie.domain.achievement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    Optional<UserAchievement> findByUser_IdAndAchievement_Id(Long userId, Long achievementId);

    long countByUser_IdAndUnlockedTrue(Long userId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_achievements (user_id, achievement_id, progress, unlocked)
            VALUES (:userId, :achievementId, 0, false)
            ON CONFLICT ON CONSTRAINT uq_user_achievement DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") Long userId, @Param("achievementId") Long achievementId);
}
