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
import br.com.fiap.soulie.infra.ai.AiValidationProperties;
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

    @Autowired
    private AiValidationProperties aiValidationProperties;

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
    public MissionValidationResponse validate(User user, Long missionId, MultipartFile photo) {
        // NAO e @Transactional de proposito. O upload da imagem (ate 10MB) e o
        // round-trip com o RabbitMQ acontecem aqui dentro; segurar uma conexao
        // do pool durante os dois esgotaria o Hikari (10 conexoes por padrao)
        // com poucas requisicoes simultaneas de rede lenta — e aí TODO endpoint
        // da API, login incluido, passaria a esperar por conexao.
        //
        // As escritas ficam em transacoes curtas, dentro do EvidenceService.
        DailyMission mission = buscarMissaoParaValidar(user, missionId);

        if (photo == null || photo.isEmpty()) {
            throw new ValidationException("Envie uma foto para comprovar a missão");
        }

        // Falha cedo e com mensagem clara se a fila estiver fora: melhor 503 que
        // um 202 prometendo uma análise que nunca vai acontecer.
        validationPublisher.exigirFilaDisponivel();

        var registrada = evidenceService.submitForDailyMission(user, mission, photo);
        Long validationId = registrada.validation().getId();

        validationService.esperarVeredito(validationId, aiValidationProperties.getWaitTimeout());
        return statusDaValidacao(user, validationId);
    }

    /**
     * Leitura e regras de periodo, fora da transacao do upload.
     *
     * <p>Sem @Transactional de proposito: chamada de dentro da propria classe
     * nao passa pelo proxy do Spring, entao a anotacao seria decorativa. Cada
     * consulta roda na transacao do proprio repository, e DailyMission nao tem
     * associacao LAZY — a entidade destacada e segura de usar aqui.
     */
    private DailyMission buscarMissaoParaValidar(User user, Long missionId) {
        DailyMission mission = dailyMissionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("Missão não encontrada"));
        if (alreadyDoneInPeriod(user.getId(), mission)) {
            throw new ValidationException(MissionPeriodWindow.alreadyDoneMessage(mission.getPeriod()));
        }
        return mission;
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

    /**
     * Diz se o texto de uma atividade livre casa com o nome de uma missão.
     *
     * <p><b>Não concede mais recompensa nenhuma.</b> A versão anterior casava
     * texto livre com o nome da missão e chamava {@code reward(...)}, creditando
     * XP, impacto, streak e conquistas <b>sem foto e sem IA</b> — bastava
     * escrever "Ir de bicicleta" numa atividade para contornar todo o pipeline
     * de validação. Enquanto essa porta existisse, o pipeline era decorativo.
     *
     * <p>Agora missão do catálogo só se conclui por
     * {@code POST /v1/missions/{id}/validate}, com foto e aprovação da IA.
     *
     * @return a missão que o texto menciona, se houver — para o chamador poder
     *         orientar o usuário em vez de registrar algo que não vale nada
     */
    @Transactional(readOnly = true)
    public Optional<DailyMission> missaoMencionadaNoTexto(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        return dailyMissionRepository.findAllByOrderByIdAsc().stream()
                .filter(item -> normalized.contains(item.getName().toLowerCase(Locale.ROOT)))
                .findFirst();
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
                validation.getResultValidation().name(),
                validation.getReason()
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
        Validation validation = validationService.findDetalheById(validationId);
        var evidence = validation.getEvidence();

        // 404 tambem para validacao de terceiro, nao 403. Responder diferente
        // transformaria a rota num oraculo: daria para confirmar a existencia de
        // um id e inferir o total e a taxa de crescimento de validacoes do
        // sistema. Nenhum dado de terceiro vazava no corpo, mas a distincao em
        // si ja era informacao.
        if (evidence.getUser() == null || !evidence.getUser().getId().equals(user.getId())) {
            throw new EntityNotFoundException("Validação não encontrada");
        }
        if (evidence.getDailyMission() == null) {
            throw new EntityNotFoundException("Validação não pertence a uma missão do app");
        }

        return validationResponse(user, evidence.getDailyMission(), List.of(), validation);
    }

    private record Reward(UserActivity activity, List<Achievement> unlocked) {
    }

}
