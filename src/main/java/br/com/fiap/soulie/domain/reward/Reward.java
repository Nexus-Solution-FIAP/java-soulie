package br.com.fiap.soulie.domain.reward;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Resgate efetuado por um usuário.
 *
 * <p>O benefício e o custo são **copiados do catálogo** no momento do resgate, em
 * vez de referenciados por ele. Isso é deliberado: é registro histórico, e se o
 * preço de um item mudar depois, o resgate antigo tem de continuar mostrando o
 * que a pessoa de fato pagou na época.
 */
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

    /** Nulo nos resgates antigos, feitos quando o preço vinha do cliente. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_catalog_id")
    private RewardCatalog rewardCatalog;

    @Column(nullable = false, length = 500)
    private String benefits;

    @Column(name = "points_spent", nullable = false)
    private int pointsSpent;

    public Reward(RewardCatalog catalog, User user) {
        this.user = user;
        this.rewardCatalog = catalog;
        this.benefits = catalog.getBenefits();
        this.pointsSpent = catalog.getCost();
    }
}
