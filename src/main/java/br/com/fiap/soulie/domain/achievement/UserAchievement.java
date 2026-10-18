package br.com.fiap.soulie.domain.achievement;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_achievements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "achievement_id"})
)
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "achievement_id")
    private Achievement achievement;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false)
    private boolean unlocked;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    public UserAchievement() {
    }

    public static UserAchievement start(User user, Achievement achievement) {
        var row = new UserAchievement();
        row.user = user;
        row.achievement = achievement;
        row.progress = 0;
        row.unlocked = false;
        return row;
    }

    public void updateProgress(int progress) {
        this.progress = Math.max(progress, 0);
    }

    public boolean unlock(LocalDateTime when) {
        if (unlocked) {
            return false;
        }
        unlocked = true;
        unlockedAt = when;
        return true;
    }
}
