package br.com.fiap.soulie.domain.social;

import br.com.fiap.soulie.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Uma pessoa segue outra. Relação unilateral e imediata.
 *
 * <p><b>Amizade não tem entidade.</b> Ela é a propriedade derivada "existem as
 * duas linhas, uma em cada sentido". Guardar amizade numa segunda tabela criaria
 * um estado para manter em sincronia com o follow, e os dois divergiriam no
 * primeiro caminho de erro — alguém deixa de seguir e a amizade fica órfã.
 *
 * <p>Seguir sozinho concede pouco de propósito: cutucada exige amizade
 * <b>mútua</b> ou mesmo jardim, e feed e ranking continuam escopados no jardim.
 */
@Getter
@Builder
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Follow")
@Table(
        name = "follows",
        // Declarados também aqui, não só na migration V23. O schema dos testes
        // vem das ENTIDADES e o de produção vem das MIGRATIONS: restrição que
        // existe só de um lado não é exercitada por teste nenhum. Foi assim que
        // o primeiro teste do CHECK passou sem provar nada.
        uniqueConstraints = @UniqueConstraint(
                name = "uq_follows_pair",
                columnNames = {"follower_id", "followee_id"})
)
@Check(name = "check_follows_not_self", constraints = "follower_id <> followee_id")
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", nullable = false)
    private User followee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Follow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
