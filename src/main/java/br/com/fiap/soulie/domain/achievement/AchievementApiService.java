package br.com.fiap.soulie.domain.achievement;

import br.com.fiap.soulie.api.TimeLabels;
import br.com.fiap.soulie.api.dto.AchievementResponse;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.mission.UserMissionCompletionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AchievementApiService {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserMissionCompletionRepository completionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Transactional
    public List<Achievement> sync(User user, Avatar avatar) {
        List<Achievement> catalog = achievementRepository.findAllByOrderByIdAsc();
        List<Achievement> unlockedNow = new ArrayList<>();
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < 4) {
            changed = false;
            userRepository.saveAndFlush(user);
            avatarRepository.saveAndFlush(avatar);
            int rank = (int) userRepository.countByActiveTrueAndPointsGreaterThan(user.getPoints()) + 1;
            for (Achievement achievement : catalog) {
                UserAchievement row = userAchievementRepository
                        .findByUser_IdAndAchievement_Id(user.getId(), achievement.getId())
                        .orElseGet(() -> userAchievementRepository.save(UserAchievement.start(user, achievement)));
                int progress = progressOf(achievement, user, avatar, rank);
                row.updateProgress(progress);
                if (achievement.reached(progress) && row.unlock(LocalDateTime.now())) {
                    user.addPoints(achievement.getXpReward());
                    avatar.addXp(achievement.getXpReward());
                    unlockedNow.add(achievement);
                    changed = true;
                }
            }
        }
        userRepository.saveAndFlush(user);
        avatarRepository.saveAndFlush(avatar);
        return unlockedNow;
    }

    @Transactional
    public List<AchievementResponse> list(User user, Avatar avatar) {
        sync(user, avatar);
        return achievementRepository.findAllByOrderByIdAsc().stream()
                .map(achievement -> toResponse(user, achievement))
                .toList();
    }

    private AchievementResponse toResponse(User user, Achievement achievement) {
        UserAchievement row = userAchievementRepository
                .findByUser_IdAndAchievement_Id(user.getId(), achievement.getId())
                .orElseGet(() -> UserAchievement.start(user, achievement));
        String date = row.isUnlocked()
                ? TimeLabels.unlockedDate(row.getUnlockedAt())
                : row.getProgress() > 0 ? "Em progresso" : "Bloqueado";
        return new AchievementResponse(
                achievement.getId(),
                achievement.getName(),
                achievement.getDescription(),
                achievement.getIcon(),
                achievement.getColor(),
                achievement.getCategory(),
                row.isUnlocked(),
                date,
                achievement.getXpReward(),
                progressLabel(achievement, row.getProgress())
        );
    }

    private int progressOf(Achievement achievement, User user, Avatar avatar, int rank) {
        return switch (achievement.getMetric()) {
            case MISSION_TAG -> (int) completionRepository.countByUser_IdAndMission_Tag(user.getId(), achievement.getMetricKey());
            case STREAK_DAYS -> user.currentStreak(java.time.LocalDate.now());
            case WATER_LITERS -> (int) Math.floor(user.getWaterSavedLiters());
            case SOULIE_LEVEL -> avatar.getLevel();
            case GLOBAL_RANK -> rank;
        };
    }

    private String progressLabel(Achievement achievement, int progress) {
        int target = achievement.getTarget();
        return switch (achievement.getMetric()) {
            case WATER_LITERS -> progress + "L / " + target + "L";
            case STREAK_DAYS -> progress + "d / " + target + "d";
            case SOULIE_LEVEL -> "Nível " + progress + " / " + target;
            case GLOBAL_RANK -> achievement.reached(progress) ? "Top " + target : "Fora do top " + target;
            case MISSION_TAG -> progress + " / " + target;
        };
    }
}
