package br.com.fiap.soulie.validations.reward;

import br.com.fiap.soulie.domain.reward.RewardCatalog;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

/**
 * Recusa o resgate quando o saldo não cobre o custo do item.
 *
 * <p>É uma checagem de cortesia, para a resposta ser clara. A garantia real
 * contra gastar mais do que se tem é o débito atômico em
 * {@code UserRepository.debitPoints} mais o {@code CHECK (points >= 0)} do banco
 * — este validator, por si, é vulnerável a corrida.
 */
@Component
public class SufficientPointsValidator implements RewardRedeemValidator {

    @Override
    public void validate(RewardCatalog catalog, User user) {
        if (user.getPoints() < catalog.getCost()) {
            throw new ValidationException("Pontos insuficientes para resgatar esta recompensa");
        }
    }
}
