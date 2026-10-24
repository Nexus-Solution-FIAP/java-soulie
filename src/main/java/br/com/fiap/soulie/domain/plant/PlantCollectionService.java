package br.com.fiap.soulie.domain.plant;

import br.com.fiap.soulie.api.dto.PlantItemResponse;
import br.com.fiap.soulie.domain.achievement.UserAchievementRepository;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.mission.UserMissionCompletionRepository;
import br.com.fiap.soulie.domain.soulie.SoulieLevel;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PlantCollectionService {

    private static final int MAX_LEVEL_XP = 200;

    @Autowired
    private UserPlantRepository userPlantRepository;

    @Autowired
    private UserMissionCompletionRepository completionRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Transactional
    public void ensureRows(User user) {
        Map<FlowerType, UserPlant> existing = index(user);
        boolean created = false;
        for (PlantCatalog item : PlantCatalog.values()) {
            if (!existing.containsKey(item.flower())) {
                userPlantRepository.save(UserPlant.locked(user, item.flower()));
                created = true;
            }
        }
        if (created) {
            userPlantRepository.flush();
        }
    }

    @Transactional
    public void persistAndEvaluate(User user, Avatar avatar) {
        ensureRows(user);
        persistCurrent(user, avatar);
        evaluateUnlocks(user);
        persistCurrent(user, avatar);
    }

    @Transactional
    public Avatar activate(User user, Avatar avatar, FlowerType flower) {
        ensureRows(user);
        persistCurrent(user, avatar);
        UserPlant row = row(user, flower);
        if (!row.isUnlocked()) {
            if (userPlantRepository.countByUser_IdAndUnlockedTrue(user.getId()) == 0) {
                chooseStarter(user, avatar, flower);
                row = row(user, flower);
            } else {
                throw new ValidationException("Planta bloqueada. " + PlantCatalog.of(flower).hint());
            }
        }
        avatar.applyPlant(flower, row.getXp());
        return avatar;
    }

    @Transactional
    public List<PlantItemResponse> catalog(User user, Avatar avatar) {
        persistAndEvaluate(user, avatar);
        FlowerType selected = avatar.getFlowerType();
        Map<FlowerType, UserPlant> existing = index(user);
        List<PlantItemResponse> items = new ArrayList<>();
        for (PlantCatalog plant : PlantCatalog.values()) {
            UserPlant row = existing.get(plant.flower());
            int xp = row == null ? 0 : row.getXp();
            int level = SoulieLevel.levelOf(xp);
            boolean unlocked = row != null && row.isUnlocked();
            items.add(new PlantItemResponse(
                    plant.flower().getValue(),
                    plant.label(),
                    plant.starterEligible(),
                    unlocked,
                    plant.flower() == selected,
                    xp,
                    level,
                    SoulieLevel.nameOf(level),
                    row == null ? null : row.getUnlockSource(),
                    unlocked ? null : plant.hint()
            ));
        }
        return items;
    }

    @Transactional
    public List<PlantItemResponse> redeemCode(User user, Avatar avatar, String code) {
        PlantCatalog plant = PlantCatalog.bySecret(code);
        if (plant == null) {
            throw new ValidationException("Código inválido");
        }
        ensureRows(user);
        UserPlant row = row(user, plant.flower());
        if (!row.isUnlocked()) {
            row.unlock(PlantUnlockKind.SECRET_CODE.name(), LocalDateTime.now());
        }
        return catalog(user, avatar);
    }

    public int unlockedCount(User user) {
        return (int) userPlantRepository.countByUser_IdAndUnlockedTrue(user.getId());
    }

    public int totalCount() {
        return PlantCatalog.values().length;
    }

    private void chooseStarter(User user, Avatar avatar, FlowerType flower) {
        PlantCatalog plant = PlantCatalog.of(flower);
        if (!plant.starterEligible()) {
            throw new ValidationException("No cadastro, escolha uma planta da flora brasileira");
        }
        UserPlant row = row(user, flower);
        row.unlock(PlantUnlockKind.STARTER.name(), LocalDateTime.now());
        row.setXp(avatar == null ? 0 : avatar.getXp());
    }

    private void persistCurrent(User user, Avatar avatar) {
        if (avatar == null || avatar.getFlowerType() == null) {
            return;
        }
        UserPlant row = row(user, avatar.getFlowerType());
        if (!row.isUnlocked()) {
            return;
        }
        row.setXp(avatar.getXp());
    }

    private void evaluateUnlocks(User user) {
        int missions = (int) completionRepository.countByUser_Id(user.getId());
        int achievements = (int) userAchievementRepository.countByUser_IdAndUnlockedTrue(user.getId());
        int maxPlants = (int) userPlantRepository.countByUser_IdAndUnlockedTrueAndXpGreaterThanEqual(user.getId(), MAX_LEVEL_XP);
        int streak = user.currentStreak(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();
        for (UserPlant row : userPlantRepository.findByUser_IdOrderByIdAsc(user.getId())) {
            if (row.isUnlocked()) {
                continue;
            }
            PlantCatalog plant = PlantCatalog.of(row.getPlantType());
            int progress = switch (plant.kind()) {
                case MISSIONS -> missions;
                case ACHIEVEMENTS -> achievements;
                case MAX_PLANTS -> maxPlants;
                case STREAK_DAYS -> streak;
                case SECRET_CODE, STARTER -> 0;
            };
            if (plant.kind() != PlantUnlockKind.SECRET_CODE && plant.kind() != PlantUnlockKind.STARTER && progress >= plant.target()) {
                row.unlock(plant.kind().name(), now);
            }
        }
    }

    private UserPlant row(User user, FlowerType flower) {
        return userPlantRepository.findByUser_IdAndPlantType(user.getId(), flower)
                .orElseGet(() -> userPlantRepository.save(UserPlant.locked(user, flower)));
    }

    private Map<FlowerType, UserPlant> index(User user) {
        Map<FlowerType, UserPlant> map = new EnumMap<>(FlowerType.class);
        for (UserPlant row : userPlantRepository.findByUser_IdOrderByIdAsc(user.getId())) {
            map.put(row.getPlantType(), row);
        }
        return map;
    }
}
