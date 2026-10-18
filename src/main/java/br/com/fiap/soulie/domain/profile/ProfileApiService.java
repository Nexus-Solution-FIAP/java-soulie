package br.com.fiap.soulie.domain.profile;

import br.com.fiap.soulie.api.TimeLabels;
import br.com.fiap.soulie.api.dto.UpdateSettingsRequest;
import br.com.fiap.soulie.api.dto.UserProfileResponse;
import br.com.fiap.soulie.api.dto.UserSettingsResponse;
import br.com.fiap.soulie.api.dto.UserStatsResponse;
import br.com.fiap.soulie.domain.achievement.AchievementApiService;
import br.com.fiap.soulie.domain.achievement.UserAchievementRepository;
import br.com.fiap.soulie.domain.garden.Garden;
import br.com.fiap.soulie.domain.soulie.SoulieApiService;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ProfileApiService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private SoulieApiService soulieApiService;

    @Autowired
    private AchievementApiService achievementApiService;

    @Transactional
    public UserProfileResponse me(User user) {
        var avatar = soulieApiService.getOrCreate(user);
        achievementApiService.sync(user, avatar);
        return build(user);
    }

    @Transactional
    public UserProfileResponse updateSettings(User user, UpdateSettingsRequest request) {
        if (request != null) {
            user.updateSettings(request.notificationsEnabled(), request.emailWeeklyDigest());
        }
        return me(user);
    }

    private UserProfileResponse build(User user) {
        int globalRank = (int) userRepository.countByActiveTrueAndPointsGreaterThan(user.getPoints()) + 1;
        Standing friends = friendsStanding(user);
        int unlocked = (int) userAchievementRepository.countByUser_IdAndUnlockedTrue(user.getId());
        boolean notifications = user.getNotificationsEnabled() == null || user.getNotificationsEnabled();
        boolean digest = Boolean.TRUE.equals(user.getEmailWeeklyDigest());
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
                        user.getWaterSavedLiters()
                ),
                new UserSettingsResponse(notifications, digest)
        );
    }

    private Standing friendsStanding(User user) {
        List<User> group = gardenMates(user);
        if (group.size() <= 1) {
            group = userRepository.findAllByActiveTrueOrderByPointsDescIdAsc();
        }
        if (group.isEmpty()) {
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

    private List<User> gardenMates(User user) {
        if (user.getGardens() == null || user.getGardens().isEmpty()) {
            return List.of(user);
        }
        return user.getGardens().stream()
                .map(Garden::getUsers)
                .flatMap(java.util.Set::stream)
                .filter(member -> member.getActive() == null || member.getActive())
                .distinct()
                .sorted(Comparator.comparingInt(User::getPoints).reversed().thenComparing(User::getId))
                .toList();
    }

    private record Standing(int position, int xpToFirst) {
    }
}
