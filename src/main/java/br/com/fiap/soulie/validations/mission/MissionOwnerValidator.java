package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.validations.evidence.EvidenceSubmissionValidator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

// Implementa as duas interfaces: a mesma regra vale para concluir a missão e para enviar evidência
@Component
public class MissionOwnerValidator implements MissionCompletionValidator, EvidenceSubmissionValidator {

    @Override
    public void validate(Mission mission, Long userId) {
        if (!mission.getAvatar().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Esta missão não pertence ao seu avatar");
        }
    }
}
