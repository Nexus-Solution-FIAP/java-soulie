package br.com.fiap.soulie.domain.reward;

import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.reward.dto.RewardResponse;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.validations.reward.RewardRedeemValidator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RewardService {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private List<RewardRedeemValidator> redeemValidators;

    // Get
    public List<RewardResponse> myRewards(Long userId) {
        return rewardRepository.findAllByUserId(userId).stream().map(RewardResponse::new).toList();
    }

    // Post - resgatar uma recompensa gastando pontos
    @Transactional
    public RewardResponse redeemReward(RewardRequest request, Long userId) {
        // Busca o usuário do banco: o do token pode estar com o saldo desatualizado
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        redeemValidators.forEach(v -> v.validate(request, user));

        user.spendPoints(request.pointsSpent());
        var reward = new Reward(request, user);
        rewardRepository.save(reward);
        return new RewardResponse(reward);
    }

}
