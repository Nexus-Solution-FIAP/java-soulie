package br.com.fiap.soulie.validations.reward;

import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.user.User;

public interface RewardRedeemValidator {

    void validate(RewardRequest request, User user);
}
