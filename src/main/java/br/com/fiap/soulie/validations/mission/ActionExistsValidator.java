package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.action.ActionRepository;
import br.com.fiap.soulie.domain.mission.dto.MissionRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActionExistsValidator implements MissionCreationValidator {

    @Autowired
    private ActionRepository actionRepository;

    @Override
    public void validate(MissionRequest request, Long userId) {
        if (!actionRepository.existsById(request.actionId())) {
            throw new EntityNotFoundException("Ação não encontrada");
        }
    }
}
