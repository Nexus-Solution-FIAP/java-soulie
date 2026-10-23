package br.com.fiap.soulie.domain.reward;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Recompensa disponível para resgate, com o preço definido pelo servidor.
 *
 * <p>Existe porque antes não existia: o {@code RewardRequest} trazia o texto do
 * benefício <b>e o preço</b> no corpo da requisição, e o único validator
 * comparava o saldo com o número que o próprio cliente havia enviado. Dava para
 * resgatar qualquer benefício por 1 ponto.
 *
 * <p>Agora o cliente só informa qual item quer; o custo e o texto vêm daqui.
 * Itens são criados e editados direto no banco (veja a migration V21), que
 * semeia o catálogo inicial.
 */
@Getter
@Builder
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "RewardCatalog")
@Table(name = "reward_catalog")
public class RewardCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String benefits;

    @Column(nullable = false)
    private int cost;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isAvailable() {
        return active == null || active;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }
}
