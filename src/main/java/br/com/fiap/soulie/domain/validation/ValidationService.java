package br.com.fiap.soulie.domain.validation;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Esta API só cria a validação como PENDING; quem aprova/reprova é a IA externa, direto no banco
@Service
public class ValidationService {

    @Autowired
    private ValidationRepository validationRepository;

    @Transactional
    public Validation createPending(Evidence evidence) {
        var validation = new Validation(evidence);
        return validationRepository.save(validation);
    }

    public ResultValidation resultOf(Long evidenceId) {
        return validationRepository.findByEvidenceId(evidenceId)
                .map(Validation::getResultValidation)
                .orElse(ResultValidation.PENDING);
    }

}
