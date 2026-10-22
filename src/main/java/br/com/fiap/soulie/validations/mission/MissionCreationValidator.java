package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.mission.dto.MissionRequest;

public interface MissionCreationValidator {

    void validate(MissionRequest request, Long userId);
}
