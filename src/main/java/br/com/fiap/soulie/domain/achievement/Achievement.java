package br.com.fiap.soulie.domain.achievement;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private String icon;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String category;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward;

    @Column(nullable = false)
    private int target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementMetric metric;

    @Column(name = "metric_key")
    private String metricKey;

    public Achievement() {
    }

    public boolean reached(int progress) {
        if (metric == AchievementMetric.GLOBAL_RANK) {
            return progress > 0 && progress <= target;
        }
        return progress >= target;
    }
}
