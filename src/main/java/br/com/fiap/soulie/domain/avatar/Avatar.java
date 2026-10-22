package br.com.fiap.soulie.domain.avatar;


import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.avatar.dto.AvatarRequest;
import br.com.fiap.soulie.domain.avatar.dto.UpdateAvatar;
import br.com.fiap.soulie.domain.soulie.SoulieLevel;
import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode
@Entity(name = "Avatar")
@Table(name = "avatars")
public class Avatar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int level;

    @Enumerated(EnumType.STRING)
    private Humor humor;

    @Enumerated(EnumType.STRING)
    private PlantType plantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "flower_type")
    private FlowerType flowerType;

    private int xp;

    @Column(name = "last_watered_at")
    private LocalDateTime lastWateredAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Avatar() {
    }

    public Avatar(AvatarRequest avatarRequest, User user) {
        this.name = avatarRequest.name();
        this.user = user;
        this.level = 0;
        this.xp = 0;
        this.humor = Humor.NEUTRAL;
        this.plantType = avatarRequest.plantType() != null ? avatarRequest.plantType() : PlantType.IPE_AMARELO;
        this.flowerType = FlowerType.GIRASSOL;
    }

    public static Avatar starter(User user) {
        Avatar avatar = new Avatar();
        avatar.setName("Meu Soulie");
        avatar.setLevel(0);
        avatar.setXp(0);
        avatar.setHumor(Humor.NEUTRAL);
        avatar.setPlantType(PlantType.IPE_AMARELO);
        avatar.setFlowerType(FlowerType.IPE_AMARELO);
        avatar.setUser(user);
        return avatar;
    }

    public void addXp(int amount) {
        if (amount <= 0) {
            return;
        }
        this.xp += amount;
        this.level = SoulieLevel.levelOf(this.xp);
    }

    public boolean markWatered(LocalDateTime now) {
        if (wasWateredOn(now.toLocalDate())) {
            return false;
        }
        this.lastWateredAt = now;
        if (humor == Humor.WITHERED || humor == Humor.SAD || humor == Humor.NEUTRAL) {
            humor = Humor.HAPPY;
        }
        return true;
    }

    public boolean wasWateredOn(LocalDate day) {
        return lastWateredAt != null && lastWateredAt.toLocalDate().equals(day);
    }

    public void rename(String newName) {
        if (newName != null && !newName.isBlank()) {
            this.name = newName.trim();
        }
    }

    public void changeFlower(FlowerType flower) {
        applyPlant(flower, this.xp);
    }

    public void applyPlant(FlowerType flower, int plantXp) {
        if (flower == null) {
            return;
        }
        this.flowerType = flower;
        this.xp = Math.max(plantXp, 0);
        this.level = SoulieLevel.levelOf(this.xp);
        this.plantType = switch (flower) {
            case IPE_AMARELO -> PlantType.IPE_AMARELO;
            case GUAIMBE_ONDULADO -> PlantType.GUAIMBE_ONDULADO;
            case MANACA_DA_SERRA -> PlantType.MANACA_DA_SERRA;
            case MARANTA -> PlantType.MARANTA;
            case PACOVA -> PlantType.PACOVA;
            case PITANGUEIRA -> PlantType.PITANGUEIRA;
            case RAINHA_DO_ABISMO -> PlantType.RAINHA_DO_ABISMO;
            case TREVO_ROXO -> PlantType.TREVO_ROXO;
            case MULUNGU -> PlantType.MULUNGU;
            case SUCUPIRA_PRETA -> PlantType.SUCUPIRA_PRETA;
            default -> this.plantType == null ? PlantType.IPE_AMARELO : this.plantType;
        };
    }

    @PrePersist
    void onCreate() {
        if (humor == null) {
            humor = Humor.NEUTRAL;
        }
        if (plantType == null) {
            plantType = PlantType.IPE_AMARELO;
        }
        if (flowerType == null) {
            flowerType = FlowerType.GIRASSOL;
        }
    }

    public void updateAvatar(@Valid UpdateAvatar updateAvatarData){
        if (updateAvatarData.name() != null){
            this.name = updateAvatarData.name();
        }

        if (updateAvatarData.plantType() != null) {
            this.plantType = updateAvatarData.plantType();
        }
    }

}
