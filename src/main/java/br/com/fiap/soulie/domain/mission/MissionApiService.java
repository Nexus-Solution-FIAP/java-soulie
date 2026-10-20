package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.api.dto.BadgeUnlockedResponse;
import br.com.fiap.soulie.api.dto.DailyMissionResponse;
import br.com.fiap.soulie.api.dto.MissionValidationResponse;
import br.com.fiap.soulie.api.dto.SuggestedMissionResponse;
import br.com.fiap.soulie.domain.achievement.Achievement;
import br.com.fiap.soulie.domain.achievement.AchievementApiService;
import br.com.fiap.soulie.domain.activity.UserActivity;
import br.com.fiap.soulie.domain.activity.UserActivityRepository;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.soulie.SoulieApiService;
import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MissionApiService {

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private UserMissionCompletionRepository completionRepository;

    @Autowired
    private UserActivityRepository activityRepository;

    @Autowired
    private AchievementApiService achievementApiService;

    @Autowired
    @Lazy
    private SoulieApiService soulieApiService;

    @Transactional(readOnly = true)
    public List<DailyMissionResponse> daily(User user) {
        return list(user);
    }

    @Transactional(readOnly = true)
    public List<DailyMissionResponse> list(User user) {
        LocalDate today = LocalDate.now();
        List<UserMissionCompletion> completions = completionRepository
                .findByUser_IdAndCompletedOnGreaterThanEqual(user.getId(), MissionPeriodWindow.start(MissionPeriod.YEARLY, today));
        return dailyMissionRepository.findAllByOrderByIdAsc().stream()
                .map(mission -> toResponse(mission, completions, today))
                .toList();
    }

    @Transactional
    public MissionValidationResponse validate(User user, Long missionId, MultipartFile photo) {
        DailyMission mission = dailyMissionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("Missão não encontrada"));
        if (alreadyDoneInPeriod(user.getId(), mission)) {
            throw new ValidationException(MissionPeriodWindow.alreadyDoneMessage(mission.getPeriod()));
        }
        String imageName = photo == null || photo.isEmpty() ? null : photo.getOriginalFilename();
        Reward reward = reward(user, mission, imageName, mission.getEmoji(), mission.getName() + " realizada");
        return validationResponse(user, mission, reward.unlocked());
    }

    @Transactional
    public ActivityCompletion completeFromActivity(User user, String label, String emoji) {
        if (label == null || label.isBlank()) {
            return ActivityCompletion.none();
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        Optional<DailyMission> mission = dailyMissionRepository.findAllByOrderByIdAsc().stream()
                .filter(item -> normalized.contains(item.getName().toLowerCase(Locale.ROOT)))
                .findFirst();
        if (mission.isEmpty()) {
            return ActivityCompletion.none();
        }
        if (alreadyDoneInPeriod(user.getId(), mission.get())) {
            return ActivityCompletion.alreadyDone();
        }
        return ActivityCompletion.completed(reward(user, mission.get(), null, emoji, label).activity());
    }

    private Reward reward(User user, DailyMission mission, String imageName, String emoji, String label) {
        LocalDateTime now = LocalDateTime.now();
        completionRepository.saveAndFlush(UserMissionCompletion.of(user, mission, now, imageName));
        Avatar avatar = soulieApiService.getOrCreate(user);
        user.addPoints(mission.getXp());
        avatar.addXp(mission.getXp());
        user.addImpact(mission.getCo2SavedKg(), mission.getWaterSavedLiters());
        user.registerActivityDay(now.toLocalDate());
        UserActivity activity = activityRepository.save(UserActivity.of(
                user,
                emoji == null ? mission.getEmoji() : emoji,
                label,
                mission.getXp(),
                now
        ));
        List<Achievement> unlocked = achievementApiService.sync(user, avatar);
        return new Reward(activity, unlocked);
    }

    private MissionValidationResponse validationResponse(User user, DailyMission mission, List<Achievement> unlocked) {
        LocalDate today = LocalDate.now();
        List<UserMissionCompletion> completions = completionRepository
                .findByUser_IdAndCompletedOnGreaterThanEqual(user.getId(), MissionPeriodWindow.start(MissionPeriod.YEARLY, today));
        SuggestedMissionResponse next = dailyMissionRepository.findAllByOrderByIdAsc().stream()
                .filter(item -> item.getPeriod() == mission.getPeriod())
                .filter(item -> !doneInPeriod(item, completions, today))
                .findFirst()
                .or(() -> dailyMissionRepository.findAllByOrderByIdAsc().stream()
                        .filter(item -> !doneInPeriod(item, completions, today))
                        .findFirst())
                .map(item -> new SuggestedMissionResponse(item.getId(), item.getName(), item.getTag(), item.getXp()))
                .orElse(null);
        BadgeUnlockedResponse badge = unlocked.isEmpty() ? null : new BadgeUnlockedResponse(
                unlocked.get(0).getId(),
                unlocked.get(0).getName(),
                unlocked.get(0).getDescription(),
                unlocked.get(0).getIcon()
        );
        return new MissionValidationResponse(
                true,
                mission.getXp(),
                mission.getId(),
                detections(mission),
                mission.getCo2SavedKg(),
                badge,
                next
        );
    }

    private DailyMissionResponse toResponse(DailyMission mission, List<UserMissionCompletion> completions, LocalDate today) {
        UserMissionCompletion latest = completions.stream()
                .filter(completion -> completion.getMission().getId().equals(mission.getId())
                        && MissionPeriodWindow.contains(mission.getPeriod(), today, completion.getCompletedOn()))
                .max(java.util.Comparator.comparing(UserMissionCompletion::getCompletedAt))
                .orElse(null);
        boolean done = latest != null;
        String completedAt = latest == null
                ? null
                : latest.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new DailyMissionResponse(
                mission.getId(),
                mission.getEmoji(),
                mission.getName(),
                mission.getTag(),
                mission.getDescription(),
                mission.getXp(),
                done,
                completedAt,
                mission.getPeriod().json()
        );
    }

    private boolean alreadyDoneInPeriod(Long userId, DailyMission mission) {
        LocalDate today = LocalDate.now();
        return completionRepository.existsByUser_IdAndMission_IdAndCompletedOnBetween(
                userId,
                mission.getId(),
                MissionPeriodWindow.start(mission.getPeriod(), today),
                MissionPeriodWindow.end(mission.getPeriod(), today)
        );
    }

    private boolean doneInPeriod(DailyMission mission, List<UserMissionCompletion> completions, LocalDate today) {
        return completions.stream().anyMatch(completion ->
                completion.getMission().getId().equals(mission.getId())
                        && MissionPeriodWindow.contains(mission.getPeriod(), today, completion.getCompletedOn()));
    }

    private List<String> detections(DailyMission mission) {
        String tag = mission.getTag().toLowerCase(Locale.ROOT);
        String name = mission.getName().toLowerCase(Locale.ROOT);
        List<String> lines;
        if (tag.contains("recicla") || name.contains("recicla")) {
            lines = List.of("Lixeiras de coleta seletiva identificadas", "Descarte correto de resíduos recicláveis");
        } else if (tag.contains("mobil") || name.contains("transporte") || name.contains("bicicleta")) {
            lines = List.of("Transporte coletivo sustentável em uso", "Deslocamento urbano de baixo carbono verificado");
        } else if (tag.contains("água") || name.contains("água") || name.contains("banho")) {
            lines = List.of("Controle consciente de fluxo de água detectado", "Redução de desperdício hídrico constatada");
        } else if (tag.contains("consumo") || name.contains("descart")) {
            lines = List.of("Item ecológico reutilizável reconhecido", "Prevenção de embalagens plásticas descartáveis");
        } else if (tag.contains("energia") || name.contains("luz") || name.contains("standby")) {
            lines = List.of("Equipamentos ou iluminação em modo eco", "Otimização de eficiência energética validada");
        } else if (tag.contains("natureza") || tag.contains("resíduo") || name.contains("planta") || name.contains("compost")) {
            lines = List.of("Vegetação e manejo do solo identificados", "Contribuição para absorção natural de carbono");
        } else {
            lines = List.of("Ação sustentável reconhecida", "Evidência ecológica registrada");
        }
        return List.of(lines.get(0), lines.get(1), "Comprovação registrada no Soulie");
    }

    private record Reward(UserActivity activity, List<Achievement> unlocked) {
    }

    public record ActivityCompletion(Kind kind, UserActivity activity) {
        public enum Kind { NONE, ALREADY_DONE, COMPLETED }

        public static ActivityCompletion none() {
            return new ActivityCompletion(Kind.NONE, null);
        }

        public static ActivityCompletion alreadyDone() {
            return new ActivityCompletion(Kind.ALREADY_DONE, null);
        }

        public static ActivityCompletion completed(UserActivity activity) {
            return new ActivityCompletion(Kind.COMPLETED, activity);
        }
    }
}
