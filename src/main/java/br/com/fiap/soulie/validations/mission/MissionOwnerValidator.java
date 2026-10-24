package br.com.fiap.soulie.validations.mission;

import br.com.fiap.soulie.domain.mission.Mission;
import br.com.fiap.soulie.validations.evidence.EvidenceSubmissionValidator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Implementa as duas interfaces: a mesma regra vale para concluir a missão e para enviar evidência
/**
 * Confere que a missão é do usuário logado.
 *
 * <p>A anotação {@code @Order} é parte da regra, não enfeite. Os validators são
 * injetados como {@code List} e iterados: sem ordem definida, uma regra de
 * estado — "esta missão não aceita mais evidências", "já existe evidência
 * pendente", "ainda não há evidência aprovada" — podia rodar <b>antes</b> da
 * checagem de posse e devolver 400 descrevendo o estado da missão <b>de outra
 * pessoa</b>. Varrendo ids, dava para mapear quantas missões existem e em que
 * estado cada uma está. Rodando primeiro, a resposta para recurso de terceiro é
 * sempre a mesma.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class MissionOwnerValidator implements MissionCompletionValidator, EvidenceSubmissionValidator {

    @Override
    public void validate(Mission mission, Long userId) {
        if (!mission.getAvatar().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Esta missão não pertence ao seu avatar");
        }
    }
}
