package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.action.Action;
import br.com.fiap.soulie.domain.action.ActionRepository;
import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarMapper;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.history_points.HistoryPointsService;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.mission.dto.MissionRequest;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.validations.mission.MissionCompletionValidator;
import br.com.fiap.soulie.validations.mission.MissionCreationValidator;
import br.com.fiap.soulie.validations.mission.MissionOwnerValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock private MissionRepository missionRepository;
    @Mock private ActionRepository actionRepository;
    @Mock private AvatarRepository avatarRepository;
    @Mock private HistoryPointsService historyPointsService;
    @Mock private MissionMapper mapper;
    @Mock private AvatarMapper avatarMapper;
    @Mock private MissionOwnerValidator ownerValidator;

    @Mock private MissionCreationValidator creationValidator1;
    @Mock private MissionCreationValidator creationValidator2;
    @Mock private MissionCompletionValidator completionValidator1;
    @Mock private MissionCompletionValidator completionValidator2;

    @InjectMocks
    private MissionService service;

    private User user;
    private Avatar avatar;
    private Action action;

    @BeforeEach
    void setUp() {
        // O Mockito não injeta List<...>; simulamos o que o Spring faz com todos os @Component
        ReflectionTestUtils.setField(service, "creationValidators", List.of(creationValidator1, creationValidator2));
        ReflectionTestUtils.setField(service, "completionValidators", List.of(completionValidator1, completionValidator2));

        user = TestData.userWithPoints(1L, 10);
        avatar = TestData.avatar(1L, user);
        action = TestData.action(2L, 40);
    }

    @Test
    void createMissionRunsValidatorsAndSaves() {
        var request = new MissionRequest(2L, "Beber água");
        when(avatarRepository.findByUserId(1L)).thenReturn(Optional.of(avatar));
        when(actionRepository.getReferenceById(2L)).thenReturn(action);

        service.createMission(request, 1L);

        verify(creationValidator1).validate(request, 1L);
        verify(creationValidator2).validate(request, 1L);
        var captor = ArgumentCaptor.forClass(Mission.class);
        verify(missionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MissionStatus.ACTIVE);
        assertThat(captor.getValue().getAvatar()).isSameAs(avatar);
        assertThat(captor.getValue().getAction()).isSameAs(action);
    }

    @Test
    void createMissionWithoutAvatarReturns404() {
        when(avatarRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMission(new MissionRequest(2L, "x"), 1L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(missionRepository, never()).save(any());
    }

    @Test
    void createMissionStopsWhenAValidatorFails() {
        var request = new MissionRequest(2L, "x");
        doThrow(new EntityNotFoundException("Ação não encontrada")).when(creationValidator1).validate(request, 1L);

        assertThatThrownBy(() -> service.createMission(request, 1L)).isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(missionRepository);
    }

    @Test
    void completeMissionGivesXpPointsAndHistory() {
        var mission = TestData.mission(5L, avatar, action, MissionStatus.IN_PROGRESS);
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission));

        service.completeMission(5L, 1L);

        verify(completionValidator1).validate(mission, 1L);
        verify(completionValidator2).validate(mission, 1L);
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.COMPLETED);
        assertThat(avatar.getXp()).isEqualTo(40);
        assertThat(user.getPoints()).isEqualTo(50);
        verify(historyPointsService).register(user, action, 40);
        verify(avatarMapper).toDetail(avatar);
    }

    @Test
    void completeMissionDoesNothingWhenAValidatorFails() {
        var mission = TestData.mission(5L, avatar, action, MissionStatus.IN_PROGRESS);
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission));
        doThrow(new ValidationException("sem aprovação")).when(completionValidator2).validate(mission, 1L);

        assertThatThrownBy(() -> service.completeMission(5L, 1L)).isInstanceOf(ValidationException.class);

        assertThat(mission.getStatus()).isEqualTo(MissionStatus.IN_PROGRESS);
        assertThat(user.getPoints()).isEqualTo(10);
        verifyNoInteractions(historyPointsService);
    }

    @Test
    void completeMissionNotFound() {
        when(missionRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeMission(5L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Missão não encontrada");
    }

    @Test
    void missionDetailsChecksOwner() {
        var mission = TestData.mission(5L, avatar, action, MissionStatus.ACTIVE);
        when(missionRepository.findById(5L)).thenReturn(Optional.of(mission));
        doThrow(new AccessDeniedException("x")).when(ownerValidator).validate(mission, 2L);

        assertThatThrownBy(() -> service.missionDetails(5L, 2L)).isInstanceOf(AccessDeniedException.class);
        verify(mapper, never()).toMissionResponse(any());
    }

    @Test
    void myMissionsMapsEveryMission() {
        var m1 = TestData.mission(5L, avatar, action, MissionStatus.ACTIVE);
        var m2 = TestData.mission(6L, avatar, action, MissionStatus.COMPLETED);
        when(missionRepository.findAllByAvatarUserId(1L)).thenReturn(List.of(m1, m2));

        var result = service.myMissions(1L);

        assertThat(result).hasSize(2);
        verify(mapper).toMissionResponse(m1);
        verify(mapper).toMissionResponse(m2);
    }
}
