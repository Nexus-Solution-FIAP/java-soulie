package br.com.fiap.soulie.domain.community;

import br.com.fiap.soulie.api.TimeLabels;
import br.com.fiap.soulie.api.dto.CreateFeedRequest;
import br.com.fiap.soulie.api.dto.FeedPostResponse;
import br.com.fiap.soulie.api.dto.LikeResponse;
import br.com.fiap.soulie.api.dto.RankingItemResponse;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.soulie.SoulieLevel;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommunityApiService {

    private static final String[] COLORS = {"#0EA5E9", "#10B981", "#F43F5E", "#8B5CF6", "#F59E0B", "#06B6D4"};

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private FeedPostRepository feedPostRepository;

    @Autowired
    private FeedLikeRepository feedLikeRepository;

    @Transactional(readOnly = true)
    public List<RankingItemResponse> ranking(User current) {
        List<User> users = userRepository.findAllByActiveTrueOrderByPointsDescIdAsc();
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
                    user.getName(),
                    initial(user.getName()),
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
        return feedPostRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> toResponse(post, current))
                .toList();
    }

    @Transactional
    public FeedPostResponse publish(User current, CreateFeedRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new ValidationException("Texto da publicação é obrigatório");
        }
        FeedPost post = feedPostRepository.save(FeedPost.create(
                current,
                request.text(),
                request.imageUri(),
                request.missionName(),
                request.xp()
        ));
        return toResponse(post, current);
    }

    @Transactional
    public LikeResponse toggleLike(User current, Long postId) {
        FeedPost post = feedPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Publicação não encontrada"));
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

    private FeedPostResponse toResponse(FeedPost post, User current) {
        User author = post.getAuthor();
        boolean liked = feedLikeRepository.findByPost_IdAndUser_Id(post.getId(), current.getId()).isPresent();
        return new FeedPostResponse(
                post.getId(),
                author.getName(),
                initial(author.getName()),
                COLORS[(int) Math.floorMod(author.getId(), COLORS.length)],
                post.getText(),
                TimeLabels.feedTime(post.getCreatedAt()),
                feedLikeRepository.countByPost_Id(post.getId()),
                0,
                liked,
                post.getImageUri(),
                post.getMissionName(),
                post.getXp()
        );
    }

    private String initial(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        return name.substring(0, 1).toUpperCase();
    }
}
