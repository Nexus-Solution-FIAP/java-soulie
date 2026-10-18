package br.com.fiap.soulie.domain.mission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserMissionCompletionRepository extends JpaRepository<UserMissionCompletion, Long> {

    Optional<UserMissionCompletion> findByUser_IdAndMission_IdAndCompletedOn(Long userId, Long missionId, LocalDate completedOn);

    List<UserMissionCompletion> findByUser_IdAndCompletedOn(Long userId, LocalDate completedOn);

    long countByUser_IdAndMission_Tag(Long userId, String tag);
}
