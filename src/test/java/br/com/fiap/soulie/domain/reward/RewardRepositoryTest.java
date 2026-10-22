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
        em.persist(new Reward(null, ana, "Caneca", 30));
        em.persist(new Reward(null, ana, "Camiseta", 80));
        em.persist(new Reward(null, bia, "Boné", 50));
        flushAndClear();

        assertThat(repository.findAllByUserId(ana.getId()))
                .extracting(Reward::getBenefits)
                .containsExactlyInAnyOrder("Caneca", "Camiseta");
    }
}
