package br.com.fiap.soulie.validations.reward;

import br.com.fiap.soulie.domain.reward.RewardCatalog;
import br.com.fiap.soulie.domain.user.User;

/**
 * Regra de resgate de recompensa.
 *
 * <p>Recebe o item do **catálogo**, não o corpo da requisição: o custo é do
 * servidor. Antes a assinatura era {@code validate(RewardRequest, User)}, e o
 * único validator comparava o saldo com o preço que o cliente havia mandado.
 */
public interface RewardRedeemValidator {

    void validate(RewardCatalog catalog, User user);
}
