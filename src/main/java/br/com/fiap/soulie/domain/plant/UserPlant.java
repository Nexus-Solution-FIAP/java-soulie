package br.com.fiap.soulie.domain.plant;

import br.com.fiap.soulie.domain.avatar.enums.FlowerType;
import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_plants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "plant_type"})
)
public class UserPlant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "plant_type", nullable = false, length = 40)
    private FlowerType plantType;

    @Column(nullable = false)
    private int xp;

    @Column(nullable = false)
    private boolean unlocked;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "unlock_source", length = 32)
    private String unlockSource;

    public UserPlant() {
    }

    public static UserPlant locked(User user, FlowerType plantType) {
        UserPlant row = new UserPlant();
        row.user = user;
        row.plantType = plantType;
        row.xp = 0;
        row.unlocked = false;
        return row;
    }

    public void unlock(String source, LocalDateTime when) {
        if (unlocked) {
            return;
        }
        unlocked = true;
        unlockSource = source;
        unlockedAt = when;
    }

    public void setXp(int xp) {
        this.xp = Math.max(xp, 0);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public FlowerType getPlantType() {
        return plantType;
    }

    public int getXp() {
        return xp;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public String getUnlockSource() {
        return unlockSource;
    }
}
