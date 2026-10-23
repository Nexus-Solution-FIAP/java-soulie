package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.validation.ValidationRepository;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Quem aprova a evidência é a IA externa; o worker da fila grava APPROVED em validations.
@Component
public class ApprovedEvidenceValidator implements MissionCompletionValidator {

    @Autowired
    private ValidationRepository validationRepository;

    @Override
    public void validate(Mission mission, Long userId) {
        if (!validationRepository.existsByEvidenceMissionIdAndResultValidation(mission.getId(), ResultValidation.APPROVED)) {
            throw new ValidationException("A missão ainda não possui evidência aprovada");
        }
    }
}
