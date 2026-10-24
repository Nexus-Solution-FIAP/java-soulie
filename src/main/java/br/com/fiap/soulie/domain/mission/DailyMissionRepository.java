package br.com.fiap.soulie.domain.mission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyMissionRepository extends JpaRepository<DailyMission, Long> {
    List<DailyMission> findAllByOrderByIdAsc();
}
