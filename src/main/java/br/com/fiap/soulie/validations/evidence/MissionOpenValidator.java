package br.com.fiap.soulie.validations.evidence;

import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class MissionOpenValidator implements EvidenceSubmissionValidator {

    @Override
    public void validate(Mission mission, Long userId) {
        if (mission.getStatus() != MissionStatus.ACTIVE && mission.getStatus() != MissionStatus.IN_PROGRESS) {
            throw new ValidationException("Esta missão não aceita mais evidências");
        }
    }
}
