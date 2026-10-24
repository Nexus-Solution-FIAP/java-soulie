package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class MissionNotFinishedValidator implements MissionCompletionValidator {

    @Override
    public void validate(Mission mission, Long userId) {
        if (mission.getStatus() == MissionStatus.COMPLETED || mission.getStatus() == MissionStatus.EXPIRED) {
            throw new ValidationException("Esta missão não pode mais ser concluída");
        }
    }
}
