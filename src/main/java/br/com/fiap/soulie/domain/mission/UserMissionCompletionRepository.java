package br.com.fiap.soulie.domain.mission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserMissionCompletionRepository extends JpaRepository<UserMissionCompletion, Long> {

    Optional<UserMissionCompletion> findByUser_IdAndMission_IdAndCompletedOn(Long userId, Long missionId, LocalDate completedOn);

    List<UserMissionCompletion> findByUser_IdAndCompletedOn(Long userId, LocalDate completedOn);

    long countByUser_IdAndMission_Tag(Long userId, String tag);

    @Query("""
            select c.mission.name, count(c)
            from UserMissionCompletion c
            where c.user.id in :userIds
            group by c.mission.name
            """)
    List<Object[]> countGroupedByMissionName(@Param("userIds") Collection<Long> userIds);
}
