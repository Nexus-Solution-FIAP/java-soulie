package br.com.fiap.soulie.validations.evidence;

import br.com.fiap.soulie.domain.mission.Mission;

public interface EvidenceSubmissionValidator {

    void validate(Mission mission, Long userId);
}
