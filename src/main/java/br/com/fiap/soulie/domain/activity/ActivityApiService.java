package br.com.fiap.soulie.domain.activity;

import br.com.fiap.soulie.api.TimeLabels;
import br.com.fiap.soulie.api.dto.ActivityResponse;
import br.com.fiap.soulie.api.dto.CreateActivityRequest;
import br.com.fiap.soulie.domain.achievement.AchievementApiService;
import br.com.fiap.soulie.domain.mission.MissionApiService;
import br.com.fiap.soulie.domain.soulie.SoulieApiService;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ActivityApiService {

    @Autowired
    private UserActivityRepository activityRepository;

    @Autowired
    private MissionApiService missionApiService;

    @Autowired
    private SoulieApiService soulieApiService;

    @Autowired
    private AchievementApiService achievementApiService;

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(User user) {
        return activityRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ActivityResponse create(User user, CreateActivityRequest request) {
        if (request == null || request.label() == null || request.label().isBlank()) {
            throw new ValidationException("Descrição da atividade é obrigatória");
        }
        var completed = missionApiService.completeFromActivity(user, request.label(), request.emoji());
        if (completed.kind() == MissionApiService.ActivityCompletion.Kind.COMPLETED) {
            return toResponse(completed.activity());
        }
        int xp = completed.kind() == MissionApiService.ActivityCompletion.Kind.ALREADY_DONE || request.xp() == null
                ? 0
                : request.xp();
        LocalDateTime now = LocalDateTime.now();
        if (xp > 0) {
            var avatar = soulieApiService.getOrCreate(user);
            user.addPoints(xp);
            avatar.addXp(xp);
            user.registerActivityDay(now.toLocalDate());
            achievementApiService.sync(user, avatar);
        }
        UserActivity activity = activityRepository.save(UserActivity.of(user, request.emoji(), request.label().trim(), xp, now));
        return toResponse(activity);
    }

    public ActivityResponse toResponse(UserActivity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getEmoji(),
                activity.getLabel(),
                activity.getXp(),
                TimeLabels.activityDate(activity.getCreatedAt()),
                activity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
