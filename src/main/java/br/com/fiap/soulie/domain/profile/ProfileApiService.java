package br.com.fiap.soulie.domain.profile;

import br.com.fiap.soulie.api.TimeLabels;
import br.com.fiap.soulie.api.dto.UpdateSettingsRequest;
import br.com.fiap.soulie.api.dto.UserDataExportResponse;
import br.com.fiap.soulie.api.dto.UserProfileResponse;
import br.com.fiap.soulie.api.dto.UserSettingsResponse;
import br.com.fiap.soulie.api.dto.UserStatsResponse;
import br.com.fiap.soulie.domain.achievement.AchievementApiService;
import br.com.fiap.soulie.domain.achievement.UserAchievementRepository;
import br.com.fiap.soulie.domain.activity.ActivityApiService;
import br.com.fiap.soulie.domain.garden.GardenService;
import br.com.fiap.soulie.domain.plant.PlantCollectionService;
import br.com.fiap.soulie.domain.soulie.SoulieApiService;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfileApiService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GardenService gardenService;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private SoulieApiService soulieApiService;

    @Autowired
    private AchievementApiService achievementApiService;

    @Autowired
    private ActivityApiService activityApiService;

    @Autowired
    private PlantCollectionService plantCollectionService;

    @Transactional
    public UserProfileResponse me(User user) {
        var avatar = soulieApiService.getOrCreate(user);
        achievementApiService.sync(user, avatar);
        return build(user);
    }

    @Transactional
    public UserProfileResponse updateSettings(User user, UpdateSettingsRequest request) {
        if (request != null) {
            user.updateSettings(
                    request.notificationsEnabled(),
                    request.emailWeeklyDigest(),
                    request.allowNudges(),
                    request.showFullName()
            );
        }
        return me(user);
    }

    @Transactional
    public UserDataExportResponse exportData(User user) {
        var avatar = soulieApiService.getOrCreate(user);
        return new UserDataExportResponse(
                build(user),
                activityApiService.list(user),
                achievementApiService.list(user, avatar)
        );
    }

    @Transactional
    public void deleteAccount(User user) {
        if (gardenService.findMine(user).isPresent()) {
            gardenService.leave(user);
        }
        user.deleteUser();
    }

    @Transactional
    public void registerPushToken(User user, String token) {
        if (token == null || token.isBlank()) {
            user.setExpoPushToken(null);
            return;
        }
        String trimmed = token.trim();
        if (!trimmed.startsWith("ExponentPushToken[") && !trimmed.startsWith("ExpoPushToken[")) {
            throw new ValidationException("Token de notificação inválido");
        }
        userRepository.findAllByExpoPushToken(trimmed).stream()
                .filter(other -> !other.getId().equals(user.getId()))
                .forEach(other -> other.setExpoPushToken(null));
        user.setExpoPushToken(trimmed);
    }

    private UserProfileResponse build(User user) {
        int globalRank = (int) userRepository.countByActiveTrueAndPointsGreaterThan(user.getPoints()) + 1;
        Standing friends = friendsStanding(user);
        int unlocked = (int) userAchievementRepository.countByUser_IdAndUnlockedTrue(user.getId());
        boolean notifications = user.getNotificationsEnabled() == null || user.getNotificationsEnabled();
        boolean digest = Boolean.TRUE.equals(user.getEmailWeeklyDigest());
        boolean nudges = user.allowsNudges();
        boolean fullName = user.getShowFullName() == null || user.getShowFullName();
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getHandle(),
                user.getEmail(),
                user.getAvatarUrl(),
                TimeLabels.memberSince(user.getCreatedAt()),
                new UserStatsResponse(
                        user.currentStreak(java.time.LocalDate.now()),
                        user.getPoints(),
                        globalRank,
                        friends.position(),
                        friends.xpToFirst(),
                        unlocked,
                        user.getCo2SavedKg(),
                        user.getWaterSavedLiters(),
                        plantCollectionService.unlockedCount(user),
                        plantCollectionService.totalCount()
                ),
                new UserSettingsResponse(notifications, digest, nudges, fullName)
        );
    }

    private Standing friendsStanding(User user) {
        List<User> group = gardenService.members(user);
        if (group.size() <= 1) {
            return new Standing(1, 0);
        }
        int leaderXp = group.get(0).getPoints();
        int position = 1;
        for (User member : group) {
            if (member.getId().equals(user.getId())) {
                break;
            }
            position++;
        }
        return new Standing(position, Math.max(0, leaderXp - user.getPoints()));
    }

    private record Standing(int position, int xpToFirst) {
    }
}
