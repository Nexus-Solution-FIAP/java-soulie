package br.com.fiap.soulie.domain.evidence;

import br.com.fiap.soulie.domain.evidence.dto.EvidenceRequest;
import br.com.fiap.soulie.domain.evidence.dto.EvidenceResponse;
import br.com.fiap.soulie.domain.mission.MissionService;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.validations.evidence.EvidenceSubmissionValidator;
import br.com.fiap.soulie.validations.mission.MissionOwnerValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EvidenceService {

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MissionService missionService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private List<EvidenceSubmissionValidator> submissionValidators;

    @Autowired
    private MissionOwnerValidator ownerValidator;

    // Get
    public List<EvidenceResponse> evidencesByMission(Long missionId, Long userId) {
        var mission = missionService.findMission(missionId);
        ownerValidator.validate(mission, userId);
        return evidenceRepository.findAllByMissionId(missionId).stream()
                .map(e -> new EvidenceResponse(e, validationService.resultOf(e.getId())))
                .toList();
    }

    // Post - a evidência fica PENDING até a IA externa validar
    @Transactional
    public EvidenceResponse submitEvidence(EvidenceRequest request, Long userId) {
        var mission = missionService.findMission(request.missionId());
        submissionValidators.forEach(v -> v.validate(mission, userId));

        var user = userRepository.getReferenceById(userId);
        var evidence = new Evidence(request, user, mission);
        evidenceRepository.save(evidence);

        var validation = validationService.createPending(evidence);
        mission.startValidation();

        return new EvidenceResponse(evidence, validation.getResultValidation());
    }
}
