package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import br.com.fiap.soulie.infra.ai.ImageValidationContract;
import br.com.fiap.soulie.infra.messaging.ValidationRequestedEvent;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Ciclo de vida da validação de uma evidência.
 *
 * <p>No repositório {@code soulie-soul-up} esta classe só sabia criar a
 * validação como PENDING, com o comentário "quem aprova/reprova é a IA externa,
 * direto no banco". Como nada nunca gravava APROVED, o
 * {@code PUT /mission/{id}/complete} era impossível de concluir. Agora ela
 * fecha o ciclo.
 */
@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    @Autowired
    private ValidationRepository validationRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Cria a validação como PENDING e agenda a análise.
     *
     * <p>O evento é publicado aqui, mas quem fala com o RabbitMQ só age depois
     * do commit — ver {@code ValidationPublisher}.
     */
    @Transactional
    public Validation createPending(Evidence evidence) {
        var validation = validationRepository.save(new Validation(evidence));
        eventPublisher.publishEvent(new ValidationRequestedEvent(validation.getId()));
        return validation;
    }

    @Transactional(readOnly = true)
    public ResultValidation resultOf(Long evidenceId) {
        return validationRepository.findByEvidenceId(evidenceId)
                .map(Validation::getResultValidation)
                .orElse(ResultValidation.PENDING);
    }

    @Transactional(readOnly = true)
    public Validation findById(Long validationId) {
        return validationRepository.findById(validationId)
                .orElseThrow(() -> new EntityNotFoundException("Validação não encontrada"));
    }

    /**
     * Grava o veredito da IA, **uma vez só**.
     *
     * <p>A transição PENDING → final é feita por um {@code UPDATE} condicional
     * no banco. Se ele não afetar nenhuma linha, outra entrega da mensagem já
     * resolveu esta validação e esta chamada não deve fazer mais nada — nem
     * creditar XP, nem notificar.
     *
     * @return vazio quando já estava resolvida; a validação atualizada quando
     *         esta chamada foi a que resolveu
     */
    @Transactional
    public Optional<Validation> resolve(Long validationId, ImageValidationContract.Result resultado) {
        ResultValidation veredito = traduzir(resultado);

        if (validationRepository.resolveIfPending(validationId, veredito) == 0) {
            log.debug("Validação {} já estava resolvida; entrega ignorada", validationId);
            return Optional.empty();
        }

        var validation = findById(validationId);
        validation.resolver(
                veredito,
                resultado == null ? List.of() : resultado.detections(),
                resultado == null ? null : resultado.confidence(),
                resultado == null ? "Não foi possível analisar a imagem" : resultado.reason(),
                resultado == null ? null : resultado.provider());
        return Optional.of(validationRepository.save(validation));
    }

    /** Marca a tentativa, para a DLQ e o reprocessamento terem histórico. */
    @Transactional
    public void registrarTentativa(Long validationId) {
        validationRepository.findById(validationId).ifPresent(validation -> {
            validation.registrarTentativa();
            validationRepository.save(validation);
        });
    }

    /**
     * Traduz o status da IA.
     *
     * <p>Qualquer coisa que não seja APPROVED ou REJECTED vira {@code FAILED},
     * não {@code REJECTED}. A diferença importa: reprovado é "a foto não
     * comprova a ação"; falhou é "não conseguimos analisar". Tratar os dois
     * igual puniria o usuário que fez a ação certa por um problema nosso.
     */
    private ResultValidation traduzir(ImageValidationContract.Result resultado) {
        if (resultado == null || resultado.status() == null) {
            return ResultValidation.FAILED;
        }
        return switch (resultado.status().trim().toUpperCase()) {
            case "APPROVED", "APROVED" -> ResultValidation.APPROVED;
            case "REJECTED", "REJECT" -> ResultValidation.REJECTED;
            default -> ResultValidation.FAILED;
        };
    }
}
