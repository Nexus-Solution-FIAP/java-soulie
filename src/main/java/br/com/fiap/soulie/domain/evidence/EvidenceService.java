package br.com.fiap.soulie.domain.evidence;

import br.com.fiap.soulie.domain.evidence.dto.EvidenceRequest;
import br.com.fiap.soulie.domain.evidence.dto.EvidenceResponse;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.MissionService;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.domain.validation.Validation;
import br.com.fiap.soulie.domain.validation.ValidationService;
import br.com.fiap.soulie.infra.storage.ImageStorage;
import br.com.fiap.soulie.infra.storage.ImageStorageException;
import br.com.fiap.soulie.infra.storage.StoredImage;
import jakarta.validation.ValidationException;
import org.springframework.web.multipart.MultipartFile;
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

    @Autowired
    private ImageStorage imageStorage;

    // Get
    // @Transactional(readOnly = true) e obrigatorio aqui, nao opcional.
    // O projeto usa spring.jpa.open-in-view: false, entao a sessao do Hibernate
    // fecha quando o metodo do repository retorna. Sem a transacao no service, a
    // entidade volta DESTACADA e o primeiro acesso a uma associacao LAZY estoura
    // LazyInitializationException — 500 na rota inteira. Havia um teste
    // (MissionLazyLoadingTest) provando que GET /mission/{id} e
    // GET /evidence/mission/{id} estavam quebrados por isso.
    @Transactional(readOnly = true)
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

    /**
     * Registra a foto enviada pelo app para uma missão do catálogo.
     *
     * <p>É o fluxo do contrato {@code /v1}, irmão do {@link #submitEvidence}
     * legado. A diferença está em quem controla a imagem: aqui o cliente envia o
     * <b>arquivo</b>, o servidor valida por magic bytes e gera a chave; no
     * legado o cliente envia uma <b>URL</b> que ele mesmo hospedou.
     *
     * <p>O upload para o bucket acontece <b>antes</b> de gravar no banco, de
     * propósito: se ele falhar, nada é persistido e o usuário recebe o erro na
     * hora. A ordem inversa deixaria evidências apontando para objetos
     * inexistentes.
     */
    @Transactional
    public EvidenciaRegistrada submitForDailyMission(User user, DailyMission mission, MultipartFile photo) {
        StoredImage imagem;
        try {
            imagem = imageStorage.store(photo, "evidencias");
        } catch (ImageStorageException exception) {
            // A mensagem aqui é sobre o arquivo que o usuário mandou, então pode
            // ir para ele: "não é uma imagem válida", "excede o tamanho".
            throw new ValidationException(exception.getMessage());
        }

        var evidence = evidenceRepository.save(Evidence.doAppParaMissaoDiaria(user, mission, imagem));
        var validation = validationService.createPending(evidence);
        return new EvidenciaRegistrada(evidence, validation);
    }

    /** Evidência recém-registrada e a validação PENDING criada para ela. */
    public record EvidenciaRegistrada(Evidence evidence, Validation validation) {
    }
}
