package br.com.fiap.soulie.domain.history_points;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HistoryPointsRepository extends JpaRepository<HistoryPoints, Long> {

    @EntityGraph(attributePaths = "action")
    List<HistoryPoints> findAllByUserIdOrderByDateDesc(Long userId);

    boolean existsByActionIdAndUserIdAndDate(Long actionId, Long userId, LocalDate date);
}
