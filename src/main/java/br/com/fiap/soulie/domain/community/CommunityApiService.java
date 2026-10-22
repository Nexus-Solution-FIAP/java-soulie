package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.api.TimeLabels;
import br.com.fiap.soulie.api.dto.GardenImpactResponse;
import br.com.fiap.soulie.api.dto.CreateCommentRequest;
import br.com.fiap.soulie.api.dto.CreateFeedRequest;
import br.com.fiap.soulie.api.dto.FeedCommentResponse;
import br.com.fiap.soulie.api.dto.FeedPostResponse;
import br.com.fiap.soulie.api.dto.LikeResponse;
import br.com.fiap.soulie.api.dto.NotificationResponse;
import br.com.fiap.soulie.api.dto.NudgeResponse;
import br.com.fiap.soulie.api.dto.RankingItemResponse;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.garden.Garden;
import br.com.fiap.soulie.domain.garden.GardenService;
import br.com.fiap.soulie.domain.social.FollowService;
import br.com.fiap.soulie.domain.mission.MissionImpact;
import br.com.fiap.soulie.domain.mission.UserMissionCompletionRepository;
import br.com.fiap.soulie.domain.soulie.SoulieLevel;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.push.ExpoPushService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommunityApiService {

    private static final int NUDGES_PER_FRIEND_PER_DAY = 3;
    private static final String[] COLORS = {"#0EA5E9", "#10B981", "#F43F5E", "#8B5CF6", "#F59E0B", "#06B6D4"};

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private FeedPostRepository feedPostRepository;

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Autowired
    private FeedCommentRepository feedCommentRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Autowired
    private GardenService gardenService;

    @Autowired
    private FollowService followService;

    @Autowired
    private UserMissionCompletionRepository completionRepository;

    @Autowired
    private ExpoPushService expoPushService;

    @Transactional(readOnly = true)
    public List<RankingItemResponse> ranking(User current) {
        List<User> users = gardenService.members(current);
        Map<Long, Avatar> avatars = avatarRepository.findByUserIdIn(users.stream().map(User::getId).toList()).stream()
                .collect(Collectors.toMap(avatar -> avatar.getUser().getId(), Function.identity(), (left, right) -> left));
        int position = 1;
        var items = new java.util.ArrayList<RankingItemResponse>();
        for (User user : users) {
            Avatar avatar = avatars.get(user.getId());
            int level = avatar == null ? 0 : SoulieLevel.levelOf(avatar.getXp());
            FlowerType flower = avatar == null ? null : avatar.getFlowerType();
            items.add(new RankingItemResponse(
                    position++,
                    user.getId(),
                    user.publicName(),
                    initial(user.publicName()),
                    user.getPoints(),
                    user.currentStreak(java.time.LocalDate.now()),
                    user.getId().equals(current.getId()),
                    level,
                    flower == null ? null : flower.getValue()
            ));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<FeedPostResponse> feed(User current) {
        return gardenService.findMine(current)
                .map(garden -> feedPostRepository.findAllByGarden_IdOrderByCreatedAtDesc(garden.getId()).stream()
                        .map(post -> toResponse(post, current))
                        .toList())
                .orElseGet(List::of);
    }

    @Transactional
    public FeedPostResponse publish(User current, CreateFeedRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new ValidationException("Texto da publicação é obrigatório");
        }
        FeedPost post = feedPostRepository.save(FeedPost.create(
                current,
                gardenService.requireGarden(current),
                request.text(),
                request.imageUri(),
                request.missionName(),
                request.xp()
        ));
        return toResponse(post, current);
    }

    @Transactional
    public LikeResponse toggleLike(User current, Long postId) {
        FeedPost post = postInGarden(current, postId);
        var existing = feedLikeRepository.findByPost_IdAndUser_Id(postId, current.getId());
        boolean liked;
        if (existing.isPresent()) {
            feedLikeRepository.delete(existing.get());
            liked = false;
        } else {
            feedLikeRepository.save(FeedLike.of(post, current));
            liked = true;
        }
        return new LikeResponse(liked, feedLikeRepository.countByPost_Id(postId));
    }

    @Transactional(readOnly = true)
    public List<FeedCommentResponse> comments(User current, Long postId) {
        FeedPost post = postInGarden(current, postId);
        return feedCommentRepository.findAllByPost_IdOrderByCreatedAtAsc(post.getId()).stream()
                .map(this::toComment)
                .toList();
    }

    @Transactional
    public FeedCommentResponse comment(User current, Long postId, CreateCommentRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new ValidationException("Escreva uma mensagem");
        }
        if (request.text().trim().length() > 1000) {
            throw new ValidationException("A mensagem pode ter no máximo 1000 caracteres");
        }
        FeedPost post = postInGarden(current, postId);
        FeedComment saved = feedCommentRepository.save(FeedComment.create(post, current, request.text()));
        return toComment(saved);
    }

    @Transactional
    public NudgeResponse nudge(User current, Long targetId) {
        if (targetId == null || targetId.equals(current.getId())) {
            throw new ValidationException("Escolha um amigo do jardim");
        }
        // Mesmo jardim OU amizade mútua. Amplia sem remover comportamento: quem
        // já cutucava pelo jardim continua cutucando. A amizade entra como
        // segundo caminho para quem não divide jardim — é o que o grafo social
        // passa a permitir.
        boolean mesmoJardim = gardenService.findMine(current)
                .map(garden -> garden.getUsers().stream()
                        .anyMatch(user -> user.getId().equals(targetId)
                                && (user.getActive() == null || user.getActive())))
                .orElse(false);
        boolean amigos = followService.saoAmigos(current.getId(), targetId);

        if (!mesmoJardim && !amigos) {
            throw new ValidationException(
                    "Vocês precisam dividir o jardim ou seguir um ao outro para cutucar");
        }
        // findByIdAndActiveTrue, nao findById: o caminho da amizade nao testava
        // conta ativa, entao cutucada atravessava conta excluida.
        User target = userRepository.findByIdAndActiveTrue(targetId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        if (!target.allowsNudges()) {
            throw new ValidationException("Essa pessoa não está recebendo cutucadas");
        }
        long used = interactionRepository.countBySender_IdAndReceiver_IdAndKindAndCreatedAtGreaterThanEqual(
                current.getId(),
                targetId,
                Interaction.NUDGE,
                LocalDate.now().atStartOfDay()
        );
        if (used >= NUDGES_PER_FRIEND_PER_DAY) {
            throw new ValidationException("Você já cutucou essa pessoa 3 vezes hoje");
        }
        interactionRepository.save(Interaction.nudge(current, target));
        if (target.getNotificationsEnabled() == null || target.getNotificationsEnabled()) {
            UserNotification note = notificationRepository.save(UserNotification.nudge(target, current.publicName()));
            ExpoPushService.Result push = expoPushService.send(
                    target.getExpoPushToken(),
                    note.getTitle(),
                    note.getBody(),
                    note.getId()
            );
            if (push == ExpoPushService.Result.UNREGISTERED) {
                target.setExpoPushToken(null);
            }
        }
        return new NudgeResponse((int) (NUDGES_PER_FRIEND_PER_DAY - used - 1));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> notifications(User current) {
        return notificationRepository.findAllByUser_IdAndReadAtIsNullOrderByCreatedAtAsc(current.getId()).stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        notification.getTitle(),
                        notification.getBody(),
                        TimeLabels.feedTime(notification.getCreatedAt())
                ))
                .toList();
    }

    @Transactional
    public void markNotificationsRead(User current, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.findAllByUser_IdAndIdIn(current.getId(), ids)
                .forEach(notification -> notification.markRead(now));
    }

    private FeedPostResponse toResponse(FeedPost post, User current) {
        User author = post.getAuthor();
        boolean liked = feedLikeRepository.findByPost_IdAndUser_Id(post.getId(), current.getId()).isPresent();
        return new FeedPostResponse(
                post.getId(),
                author.publicName(),
                initial(author.publicName()),
                COLORS[(int) Math.floorMod(author.getId(), COLORS.length)],
                post.getText(),
                TimeLabels.feedTime(post.getCreatedAt()),
                feedLikeRepository.countByPost_Id(post.getId()),
                (int) feedCommentRepository.countByPost_Id(post.getId()),
                liked,
                post.getImageUri(),
                post.getMissionName(),
                post.getXp()
        );
    }

    @Transactional(readOnly = true)
    public GardenImpactResponse gardenImpact(User current) {
        var garden = gardenService.findMine(current);
        if (garden.isEmpty()) {
            return new GardenImpactResponse(0, 0, 0, 0, 0);
        }
        List<Long> memberIds = garden.get().getUsers().stream()
                .filter(member -> member.getActive() == null || member.getActive())
                .map(User::getId)
                .toList();
        if (memberIds.isEmpty()) {
            return new GardenImpactResponse(0, 0, 0, 0, 0);
        }

        double co2 = 0;
        double recycled = 0;
        double transit = 0;
        double water = 0;
        long actions = 0;
        for (Object[] row : completionRepository.countGroupedByMissionName(memberIds)) {
            String name = (String) row[0];
            long count = ((Number) row[1]).longValue();
            MissionImpact.Factors factors = MissionImpact.of(name);
            actions += count;
            co2 += factors.co2Kg() * count;
            recycled += factors.recycledKg() * count;
            water += factors.waterLiters() * count;
            if ("Transporte Público".equals(name)) {
                transit += factors.co2Kg() * count;
            }
        }
        return new GardenImpactResponse(round(co2), round(recycled), round(transit), round(water), actions);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private FeedPost postInGarden(User current, Long postId) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Publicação não encontrada"));
        Garden garden = gardenService.findMine(current)
                .orElseThrow(() -> new ValidationException("Entre em um jardim para ver essa publicação"));
        if (post.getGarden() == null || !post.getGarden().getId().equals(garden.getId())) {
            throw new ValidationException("Essa publicação não é do seu jardim");
        }
        return post;
    }

    private FeedCommentResponse toComment(FeedComment comment) {
        User author = comment.getAuthor();
        return new FeedCommentResponse(
                comment.getId(),
                author.publicName(),
                initial(author.publicName()),
                comment.getText(),
                TimeLabels.feedTime(comment.getCreatedAt())
        );
    }

    private String initial(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        return name.substring(0, 1).toUpperCase();
    }
}
