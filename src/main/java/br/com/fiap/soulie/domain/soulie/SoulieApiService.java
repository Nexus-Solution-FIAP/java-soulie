package br.com.fiap.soulie.domain.soulie;

import br.com.fiap.soulie.api.dto.SoulieResponse;
import br.com.fiap.soulie.api.dto.UpdateSoulieRequest;
import br.com.fiap.soulie.api.dto.WaterResponse;
import br.com.fiap.soulie.domain.achievement.AchievementApiService;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SoulieApiService {

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private AchievementApiService achievementApiService;

    @Transactional
    public Avatar getOrCreate(User user) {
        return avatarRepository.findByUserId(user.getId())
                .orElseGet(() -> avatarRepository.save(Avatar.starter(user)));
    }

    @Transactional
    public SoulieResponse mine(User user) {
        return toResponse(getOrCreate(user), user);
    }

    @Transactional
    public SoulieResponse update(User user, UpdateSoulieRequest request) {
        Avatar avatar = getOrCreate(user);
        if (request != null) {
            avatar.rename(request.name());
            if (request.flowerType() != null && !request.flowerType().isBlank()) {
                try {
                    avatar.changeFlower(FlowerType.from(request.flowerType()));
                } catch (IllegalArgumentException exception) {
                    throw new ValidationException(exception.getMessage());
                }
            }
        }
        return toResponse(avatar, user);
    }

    @Transactional
    public WaterResponse water(User user) {
        Avatar avatar = getOrCreate(user);
        LocalDateTime now = LocalDateTime.now();
        boolean wateredNow = avatar.markWatered(now);
        if (wateredNow) {
            user.registerActivityDay(now.toLocalDate());
            achievementApiService.sync(user, avatar);
        }
        int streak = user.currentStreak(now.toLocalDate());
        String message = wateredNow
                ? "💧 Seu Soulie foi regado! Sequência: " + streak + (streak == 1 ? " dia." : " dias.")
                : "Seu Soulie já foi regado hoje. Sequência: " + streak + (streak == 1 ? " dia." : " dias.");
        return new WaterResponse(true, message, streak);
    }

    public SoulieResponse toResponse(Avatar avatar, User user) {
        int level = SoulieLevel.levelOf(avatar.getXp());
        String lastWatered = avatar.getLastWateredAt() == null
                ? null
                : avatar.getLastWateredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new SoulieResponse(
                avatar.getId(),
                user.getId(),
                avatar.getName(),
                avatar.getFlowerType(),
                level,
                SoulieLevel.nameOf(level),
                avatar.getXp(),
                SoulieLevel.nextXp(level),
                avatar.wasWateredOn(LocalDateTime.now().toLocalDate()),
                lastWatered
        );
    }
}
