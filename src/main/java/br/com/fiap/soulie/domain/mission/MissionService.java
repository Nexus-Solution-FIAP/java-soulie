package br.com.fiap.soulie.domain.mission;

import br.com.fiap.soulie.domain.action.ActionRepository;
import br.com.fiap.soulie.domain.avatar.AvatarMapper;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.history_points.HistoryPointsService;
import br.com.fiap.soulie.domain.mission.dto.MissionRequest;
import br.com.fiap.soulie.domain.mission.dto.MissionResponse;
import br.com.fiap.soulie.validations.mission.MissionCompletionValidator;
import br.com.fiap.soulie.validations.mission.MissionCreationValidator;
import br.com.fiap.soulie.validations.mission.MissionOwnerValidator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MissionService {

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private AvatarRepository avatarRepository;

    @Autowired
    private HistoryPointsService historyPointsService;

    @Autowired
    private MissionMapper mapper;

    @Autowired
    private AvatarMapper avatarMapper;

    // O Spring injeta TODAS as classes @Component que implementam cada interface
    @Autowired
    private List<MissionCreationValidator> creationValidators;

    @Autowired
    private List<MissionCompletionValidator> completionValidators;

    @Autowired
    private MissionOwnerValidator ownerValidator;

    // Get
    // @Transactional(readOnly = true) e obrigatorio aqui, nao opcional.
    // O projeto usa spring.jpa.open-in-view: false, entao a sessao do Hibernate
    // fecha quando o metodo do repository retorna. Sem a transacao no service, a
    // entidade volta DESTACADA e o primeiro acesso a uma associacao LAZY estoura
    // LazyInitializationException — 500 na rota inteira. Havia um teste
    // (MissionLazyLoadingTest) provando que GET /mission/{id} e
    // GET /evidence/mission/{id} estavam quebrados por isso.
    @Transactional(readOnly = true)
    public List<MissionResponse> myMissions(Long userId) {
        return missionRepository.findAllByAvatarUserId(userId).stream().map(mapper::toMissionResponse).toList();
    }

    @Transactional(readOnly = true)
    public MissionResponse missionDetails(Long missionId, Long userId) {
        var mission = findMission(missionId);
        ownerValidator.validate(mission, userId);
        return mapper.toMissionResponse(mission);
    }

    // Post
    @Transactional
    public MissionResponse createMission(MissionRequest request, Long userId) {
        creationValidators.forEach(v -> v.validate(request, userId));

        var avatar = avatarRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não possui Avatar"));
        var action = actionRepository.getReferenceById(request.actionId());

        var mission = new Mission(request, action, avatar);
        missionRepository.save(mission);
        return mapper.toMissionResponse(mission);
    }

    // Put - só funciona depois que a IA aprovou uma evidência da missão
    @Transactional
    public AvatarResponse completeMission(Long missionId, Long userId) {
        var mission = findMission(missionId);
        completionValidators.forEach(v -> v.validate(mission, userId));

        mission.complete();

        var user = mission.getAvatar().getUser();
        var points = mission.getAction().getPoints();
        user.addPoints(points);
        historyPointsService.register(user, mission.getAction(), points);

        return avatarMapper.toDetail(mission.getAvatar());
    }

    @Transactional(readOnly = true)
    public Mission findMission(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("Missão não encontrada"));
    }

}
