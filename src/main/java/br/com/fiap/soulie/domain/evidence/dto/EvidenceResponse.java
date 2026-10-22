package br.com.fiap.soulie.domain.evidence.dto;

import br.com.fiap.soulie.domain.evidence.Evidence;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;

public record EvidenceResponse(

        Long id,

        Long missionId,

        String image,

        ResultValidation resultValidation
) {
    public EvidenceResponse(Evidence evidence, ResultValidation resultValidation) {
        this(
                evidence.getId(),
                evidence.getMission().getId(),
                evidence.getImage(),
                resultValidation
        );
    }
}
