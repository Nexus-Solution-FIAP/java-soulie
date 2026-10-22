package br.com.fiap.soulie.domain.reward;

import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Reward")
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 500)
    private String benefits;
    @Column(name = "points_spent", nullable = false)
    private int pointsSpent;


    public Reward(RewardRequest rewardRequest, User user) {
        this.user = user;
        this.benefits = rewardRequest.benefits();
        this.pointsSpent = rewardRequest.pointsSpent();
    }

}
