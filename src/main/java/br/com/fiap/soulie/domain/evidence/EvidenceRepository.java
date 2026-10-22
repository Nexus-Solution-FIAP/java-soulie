package br.com.fiap.soulie.domain.evidence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findAllByMissionId(Long missionId);
}
