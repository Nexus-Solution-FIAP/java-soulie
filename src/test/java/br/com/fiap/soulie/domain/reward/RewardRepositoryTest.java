package br.com.fiap.soulie.domain.reward;

import br.com.fiap.soulie.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RewardRepositoryTest extends RepositoryTestBase {

    @Autowired
    private RewardRepository repository;

    @Test
    void findAllByUserId() {
        var ana = persistUser("ana@soulie.com", true);
        var bia = persistUser("bia@soulie.com", true);
        var caneca = persistRewardCatalog("Caneca", 30);
        var camiseta = persistRewardCatalog("Camiseta", 80);
        var bone = persistRewardCatalog("Boné", 50);
        em.persist(new Reward(caneca, ana));
        em.persist(new Reward(camiseta, ana));
        em.persist(new Reward(bone, bia));
        flushAndClear();

        assertThat(repository.findAllByUserId(ana.getId()))
                .extracting(Reward::getBenefits)
                .containsExactlyInAnyOrder("Caneca", "Camiseta");
    }

    @Test
    void rewardCopiesBenefitsAndCostFromCatalog() {
        var ana = persistUser("copia@soulie.com", true);
        var item = persistRewardCatalog("Vaso decorado", 250);

        var reward = em.persist(new Reward(item, ana));
        flushAndClear();

        // O resgate guarda uma COPIA do texto e do custo, nao uma referencia
        // viva: se o preco do item mudar depois, o historico tem de continuar
        // mostrando o que a pessoa pagou na epoca.
        assertThat(repository.findById(reward.getId())).get()
                .satisfies(r -> {
                    assertThat(r.getBenefits()).isEqualTo("Vaso decorado");
                    assertThat(r.getPointsSpent()).isEqualTo(250);
                });
    }
}
