package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.RepositoryTestBase;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationRepositoryTest extends RepositoryTestBase {

    @Autowired
    private ValidationRepository repository;

    @Test
    void existsByEvidenceMissionIdAndResultValidation() {
        var ana = persistUser("ana@soulie.com", true);
        var avatar = persistAvatar(ana);
        var action = persistAction("Meditar", 20);
        var mission = persistMission(avatar, action, MissionStatus.IN_PROGRESS);
        var otherMission = persistMission(avatar, action, MissionStatus.IN_PROGRESS);

        persistValidation(persistEvidence(mission, ana), ResultValidation.REJECTED);
        persistValidation(persistEvidence(mission, ana), ResultValidation.APPROVED);
        persistValidation(persistEvidence(otherMission, ana), ResultValidation.PENDING);
        flushAndClear();

        assertThat(repository.existsByEvidenceMissionIdAndResultValidation(mission.getId(), ResultValidation.APPROVED)).isTrue();
        assertThat(repository.existsByEvidenceMissionIdAndResultValidation(mission.getId(), ResultValidation.PENDING)).isFalse();
        assertThat(repository.existsByEvidenceMissionIdAndResultValidation(otherMission.getId(), ResultValidation.PENDING)).isTrue();
        assertThat(repository.existsByEvidenceMissionIdAndResultValidation(otherMission.getId(), ResultValidation.APPROVED)).isFalse();
    }

    @Test
    void findByEvidenceId() {
        var ana = persistUser("ana@soulie.com", true);
        var mission = persistMission(persistAvatar(ana), persistAction("Meditar", 20), MissionStatus.IN_PROGRESS);
        var evidence = persistEvidence(mission, ana);
        persistValidation(evidence, ResultValidation.APPROVED);
        flushAndClear();

        assertThat(repository.findByEvidenceId(evidence.getId())).get()
                .extracting(Validation::getResultValidation).isEqualTo(ResultValidation.APPROVED);
        assertThat(repository.findByEvidenceId(999L)).isEmpty();
    }
}
