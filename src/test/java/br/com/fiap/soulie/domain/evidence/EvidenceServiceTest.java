package br.com.fiap.soulie.domain.evidence;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.evidence.dto.EvidenceRequest;
import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.domain.mission.MissionService;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.domain.validation.Validation;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import br.com.fiap.soulie.validations.evidence.EvidenceSubmissionValidator;
import br.com.fiap.soulie.validations.mission.MissionOwnerValidator;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock private EvidenceRepository evidenceRepository;
    @Mock private UserRepository userRepository;
    @Mock private MissionService missionService;
    @Mock private ValidationService validationService;
    @Mock private MissionOwnerValidator ownerValidator;
    @Mock private EvidenceSubmissionValidator validator1;
    @Mock private EvidenceSubmissionValidator validator2;

    @InjectMocks
    private EvidenceService service;

    private User user;
    private Mission mission;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "submissionValidators", List.of(validator1, validator2));
        user = TestData.user(1L);
        mission = TestData.mission(3L, TestData.avatar(1L, user), TestData.action(1L, 10), MissionStatus.ACTIVE);
    }

    @Test
    void submitCreatesPendingValidationAndStartsMission() {
        var request = new EvidenceRequest(3L, "https://img/foto.png");
        when(missionService.findMission(3L)).thenReturn(mission);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(validationService.createPending(any())).thenAnswer(inv -> new Validation(inv.getArgument(0)));

        var response = service.submitEvidence(request, 1L);

        verify(validator1).validate(mission, 1L);
        verify(validator2).validate(mission, 1L);
        var captor = ArgumentCaptor.forClass(Evidence.class);
        verify(evidenceRepository).save(captor.capture());
        assertThat(captor.getValue().getImage()).isEqualTo("https://img/foto.png");
        assertThat(captor.getValue().getMission()).isSameAs(mission);
        verify(validationService).createPending(captor.getValue());

        assertThat(mission.getStatus()).isEqualTo(MissionStatus.IN_PROGRESS);
        assertThat(response.resultValidation()).isEqualTo(ResultValidation.PENDING);
        assertThat(response.missionId()).isEqualTo(3L);
    }

    @Test
    void submitDoesNotSaveWhenAValidatorFails() {
        when(missionService.findMission(3L)).thenReturn(mission);
        doThrow(new ValidationException("pendente")).when(validator1).validate(mission, 1L);

        assertThatThrownBy(() -> service.submitEvidence(new EvidenceRequest(3L, "img"), 1L))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(evidenceRepository, validationService);
        assertThat(mission.getStatus()).isEqualTo(MissionStatus.ACTIVE);
    }

    @Test
    void evidencesByMissionReturnsResultOfEachEvidence() {
        var evidence = Evidence.builder()
                .id(9L).user(user).mission(mission).image("https://img/foto.png").build();
        when(missionService.findMission(3L)).thenReturn(mission);
        when(evidenceRepository.findAllByMissionId(3L)).thenReturn(List.of(evidence));
        when(validationService.resultOf(9L)).thenReturn(ResultValidation.APPROVED);

        var result = service.evidencesByMission(3L, 1L);

        verify(ownerValidator).validate(mission, 1L);
        assertThat(result).singleElement()
                .satisfies(r -> assertThat(r.resultValidation()).isEqualTo(ResultValidation.APPROVED));
    }
}
