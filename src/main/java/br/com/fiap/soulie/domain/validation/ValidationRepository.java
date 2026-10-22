package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ValidationRepository extends JpaRepository<Validation, Long> {

    boolean existsByEvidenceMissionIdAndResultValidation(Long missionId, ResultValidation resultValidation);

    Optional<Validation> findByEvidenceId(Long evidenceId);
}
