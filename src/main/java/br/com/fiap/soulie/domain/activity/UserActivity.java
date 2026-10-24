package br.com.fiap.soulie.domain.activity;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_activities")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 16)
    private String emoji;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int xp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UserActivity() {
    }

    public static UserActivity of(User user, String emoji, String label, int xp, LocalDateTime when) {
        var activity = new UserActivity();
        activity.user = user;
        activity.emoji = emoji == null || emoji.isBlank() ? "🌱" : emoji;
        activity.label = label;
        activity.xp = Math.max(xp, 0);
        activity.createdAt = when;
        return activity;
    }
}
