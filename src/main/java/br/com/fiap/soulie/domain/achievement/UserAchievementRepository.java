package br.com.fiap.soulie.domain.achievement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    Optional<UserAchievement> findByUser_IdAndAchievement_Id(Long userId, Long achievementId);

    long countByUser_IdAndUnlockedTrue(Long userId);
}
