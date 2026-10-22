package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.history_points.HistoryPointsRepository;
import br.com.fiap.soulie.domain.mission.Mission;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// O banco só permite um registro de pontos por (ação, usuário, dia) - ver V8
@Component
public class ActionNotDoneTodayValidator implements MissionCompletionValidator {

    @Autowired
    private HistoryPointsRepository historyPointsRepository;

    @Override
    public void validate(Mission mission, Long userId) {
        if (historyPointsRepository.existsByActionIdAndUserIdAndDate(mission.getAction().getId(), userId, LocalDate.now())) {
            throw new ValidationException("Você já pontuou com esta ação hoje");
        }
    }
}
