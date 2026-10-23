package br.com.fiap.soulie.domain.reward;

import br.com.fiap.soulie.domain.reward.dto.RewardCatalogResponse;
import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.reward.dto.RewardResponse;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.exception.BusinessException;
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
    private RewardCatalogRepository catalogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private List<RewardRedeemValidator> redeemValidators;

    // Get
    @Transactional(readOnly = true)
    public List<RewardResponse> myRewards(Long userId) {
        int saldoAtual = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"))
                .getPoints();
        return rewardRepository.findAllByUserId(userId).stream()
                .map(reward -> RewardResponse.of(reward, saldoAtual))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RewardCatalogResponse> catalog() {
        return catalogRepository.findAllByActiveTrueOrderByCostAsc().stream()
                .map(RewardCatalogResponse::new)
                .toList();
    }

    /**
     * Resgata uma recompensa do catálogo.
     *
     * <p>A ordem aqui importa e não é acidental:
     *
     * <ol>
     *   <li>o item vem do <b>catálogo</b>, nunca do corpo da requisição — é o que
     *       impede o cliente de escolher o preço;</li>
     *   <li>os validators rodam antes, só para dar uma mensagem clara de saldo
     *       insuficiente;</li>
     *   <li>o débito é <b>atômico no banco</b>. Esse é o passo que de fato
     *       protege: validar e depois subtrair em memória é uma corrida, em que
     *       duas requisições simultâneas gastam o mesmo saldo. Se o débito não
     *       afetar nenhuma linha, o saldo mudou no meio do caminho e o resgate é
     *       abortado com 409.</li>
     * </ol>
     */
    @Transactional
    public RewardResponse redeemReward(RewardRequest request, Long userId) {
        var catalog = catalogRepository.findById(request.rewardCatalogId())
                .orElseThrow(() -> new EntityNotFoundException("Recompensa não encontrada"));
        if (!catalog.isAvailable()) {
            throw new BusinessException("Esta recompensa não está mais disponível");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        redeemValidators.forEach(v -> v.validate(catalog, user));

        if (userRepository.debitPoints(userId, catalog.getCost()) == 0) {
            throw BusinessException.conflito("Saldo de pontos insuficiente para resgatar esta recompensa");
        }

        var reward = rewardRepository.save(new Reward(catalog, user));

        // O débito foi uma atualização em massa, que não reflete na entidade já
        // carregada: o saldo tem de ser lido de novo do banco.
        int saldoRestante = userRepository.findById(userId)
                .map(atualizado -> atualizado.getPoints())
                .orElse(0);
        return RewardResponse.of(reward, saldoRestante);
    }
}
