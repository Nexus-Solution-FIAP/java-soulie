package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.RepositoryTestBase;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class MissionRepositoryTest extends RepositoryTestBase {

    @Autowired
    private MissionRepository repository;

    @Test
    void findAllByAvatarUserIdReturnsOnlyUserMissions() {
        var ana = persistUser("ana@soulie.com", true);
        var bia = persistUser("bia@soulie.com", true);
        var action = persistAction("Meditar", 20);
        persistMission(persistAvatar(ana), action, MissionStatus.ACTIVE);
        persistMission(persistAvatar(bia), action, MissionStatus.ACTIVE);
        flushAndClear();

        var missions = repository.findAllByAvatarUserId(ana.getId());

        assertThat(missions).singleElement()
                .satisfies(m -> assertThat(m.getAction().getName()).isEqualTo("Meditar"));
    }

    @Test
    void statusIsStoredAsText() {
        var ana = persistUser("ana@soulie.com", true);
        var mission = persistMission(persistAvatar(ana), persistAction("Meditar", 20), MissionStatus.IN_PROGRESS);
        flushAndClear();

        assertThat(repository.findById(mission.getId())).get()
                .extracting(Mission::getStatus).isEqualTo(MissionStatus.IN_PROGRESS);
    }
}
