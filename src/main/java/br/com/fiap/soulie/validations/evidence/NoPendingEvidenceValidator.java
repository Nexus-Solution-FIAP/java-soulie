package br.com.fiap.soulie.validations.evidence;

import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.validation.ValidationRepository;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoPendingEvidenceValidator implements EvidenceSubmissionValidator {

    @Autowired
    private ValidationRepository validationRepository;

    @Override
    public void validate(Mission mission, Long userId) {
        if (validationRepository.existsByEvidenceMissionIdAndResultValidation(mission.getId(), ResultValidation.PENDING)) {
            throw new ValidationException("Já existe uma evidência aguardando validação para esta missão");
        }
    }
}
