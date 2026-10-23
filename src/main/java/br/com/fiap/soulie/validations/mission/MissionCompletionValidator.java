package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.mission.Mission;

public interface MissionCompletionValidator {

    void validate(Mission mission, Long userId);
}
