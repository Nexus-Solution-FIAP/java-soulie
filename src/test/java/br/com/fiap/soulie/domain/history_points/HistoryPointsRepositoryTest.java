package br.com.fiap.soulie.domain.history_points;

import br.com.fiap.soulie.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryPointsRepositoryTest extends RepositoryTestBase {

    @Autowired
    private HistoryPointsRepository repository;

    @Test
    void existsByActionIdAndUserIdAndDate() {
        var ana = persistUser("ana@soulie.com", true);
        var meditar = persistAction("Meditar", 20);
        var correr = persistAction("Correr", 30);
        em.persist(new HistoryPoints(null, meditar, ana, LocalDate.now(), 20));
        flushAndClear();

        var today = LocalDate.now();
        assertThat(repository.existsByActionIdAndUserIdAndDate(meditar.getId(), ana.getId(), today)).isTrue();
        assertThat(repository.existsByActionIdAndUserIdAndDate(meditar.getId(), ana.getId(), today.minusDays(1))).isFalse();
        assertThat(repository.existsByActionIdAndUserIdAndDate(correr.getId(), ana.getId(), today)).isFalse();
    }

    @Test
    void findAllByUserIdOrderByDateDescReturnsNewestFirst() {
        var ana = persistUser("ana@soulie.com", true);
        var bia = persistUser("bia@soulie.com", true);
        var meditar = persistAction("Meditar", 20);
        em.persist(new HistoryPoints(null, meditar, ana, LocalDate.of(2026, 10, 1), 20));
        em.persist(new HistoryPoints(null, meditar, ana, LocalDate.of(2026, 10, 10), 20));
        em.persist(new HistoryPoints(null, meditar, bia, LocalDate.of(2026, 10, 5), 20));
        flushAndClear();

        var history = repository.findAllByUserIdOrderByDateDesc(ana.getId());

        assertThat(history).extracting(HistoryPoints::getDate)
                .containsExactly(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1));
    }
}
