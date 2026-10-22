package br.com.fiap.soulie.validations.reward;

import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class SufficientPointsValidator implements RewardRedeemValidator {

    @Override
    public void validate(RewardRequest request, User user) {
        if (user.getPoints() < request.pointsSpent()) {
            throw new ValidationException("Pontos insuficientes para resgatar esta recompensa");
        }
    }
}
