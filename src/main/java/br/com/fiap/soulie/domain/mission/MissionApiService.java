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
import br.com.fiap.soulie.domain.evidence.EvidenceService;
import br.com.fiap.soulie.domain.validation.Validation;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.infra.exception.BusinessException;
import br.com.fiap.soulie.infra.messaging.ValidationPublisher;
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

    @Autowired
    @Lazy
    private EvidenceService evidenceService;

    @Autowired
    private ValidationPublisher validationPublisher;

    @Autowired
    @Lazy
    private ValidationService validationService;

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

    /**
     * Recebe a foto da missão e a coloca na fila de validação.
     *
     * <p><b>Mudança central da Fase 4.</b> A versão anterior guardava apenas
     * {@code photo.getOriginalFilename()}, <b>descartava os bytes</b> e creditava
     * o XP na hora — inclusive quando nenhuma foto era enviada, porque o
     * parâmetro era opcional. O campo {@code aiDetections} da resposta vinha de
     * uma tabela de frases fixas escolhidas pela tag da missão, apresentada ao
     * usuário como análise de imagem.
     *
     * <p>Agora: a imagem é validada por magic bytes, guardada no bucket com
     * chave gerada pelo servidor, registrada como {@code Evidence} com uma
     * {@code Validation} PENDING, e publicada na fila. O XP só é creditado
     * quando a IA aprovar, em {@code ValidationOutcomeService}.
     *
     * <p>A resposta mantém todos os campos que o app já consome; entram
     * {@code validationId} e {@code status}. Um cliente antigo lê
     * {@code xpGained: 0} e não estoura.
     */
    @Transactional
    public MissionValidationResponse validate(User user, Long missionId, MultipartFile photo) {
        DailyMission mission = dailyMissionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("Missão não encontrada"));
        if (alreadyDoneInPeriod(user.getId(), mission)) {
            throw new ValidationException(MissionPeriodWindow.alreadyDoneMessage(mission.getPeriod()));
        }
        if (photo == null || photo.isEmpty()) {
            throw new ValidationException("Envie uma foto para comprovar a missão");
        }

        // Falha cedo e com mensagem clara se a fila estiver fora: melhor 503 que
        // um 202 prometendo uma análise que nunca vai acontecer.
        validationPublisher.exigirFilaDisponivel();

        var registrada = evidenceService.submitForDailyMission(user, mission, photo);
        var validation = registrada.validation();

        return MissionValidationResponse.pendente(validation.getId(), mission.getId());
    }

    /**
     * Credita a recompensa depois que a IA aprovou a evidência.
     *
     * <p>Chamado pelo {@code ValidationOutcomeService}, fora da requisição
     * original. Reaproveita o mesmo {@code reward(...)} do fluxo de atividade,
     * para não existirem duas regras de recompensa divergindo com o tempo.
     */
    @Transactional
    public void rewardAprovada(User user, DailyMission mission) {
        if (alreadyDoneInPeriod(user.getId(), mission)) {
            // Entre o envio e a aprovação o usuário pode ter concluído a mesma
            // missão por outro caminho. Creditar de novo seria XP em dobro.
            return;
        }
        reward(user, mission, null, mission.getEmoji(), mission.getName() + " realizada");
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

    /**
     * Monta a resposta de consulta de uma validação já analisada.
     *
     * <p>As detecções vêm da {@code Validation}, isto é, do que a IA de fato
     * reconheceu. Antes vinham do método {@code detections(DailyMission)}, uma
     * tabela de frases fixas escolhidas por palavra-chave na tag da missão —
     * que nunca olhava a foto e respondia igual mesmo sem foto nenhuma. Esse
     * método foi removido.
     */
    private MissionValidationResponse validationResponse(User user,
                                                         DailyMission mission,
                                                         List<Achievement> unlocked,
                                                         Validation validation) {
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
        boolean aprovada = validation.isApproved();
        return new MissionValidationResponse(
                aprovada,
                aprovada ? mission.getXp() : 0,
                mission.getId(),
                validation.detectionList(),
                aprovada ? mission.getCo2SavedKg() : 0,
                badge,
                next,
                validation.getId(),
                validation.getResultValidation().name()
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

    /**
     * Estado atual de uma validação, para o app consultar depois do 202.
     *
     * <p>Confere a posse: a validação tem de ser de uma evidência do próprio
     * usuário, senão qualquer um varreria ids alheios.
     */
    @Transactional(readOnly = true)
    public MissionValidationResponse statusDaValidacao(User user, Long validationId) {
        Validation validation = validationService.findById(validationId);
        var evidence = validation.getEvidence();

        if (evidence.getUser() == null || !evidence.getUser().getId().equals(user.getId())) {
            throw BusinessException.acessoNegado("Esta validação não é sua");
        }
        if (evidence.getDailyMission() == null) {
            throw new EntityNotFoundException("Validação não pertence a uma missão do app");
        }

        return validationResponse(user, evidence.getDailyMission(), List.of(), validation);
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
