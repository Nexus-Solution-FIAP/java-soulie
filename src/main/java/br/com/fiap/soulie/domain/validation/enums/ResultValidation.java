package br.com.fiap.soulie.domain.validation.enums;

/**
 * Resultado da validação de uma evidência pela IA externa.
 *
 * <p>No repositório {@code soulie-soul-up} os valores eram {@code APROVED}
 * (sem o segundo P) e {@code REJECT}, e a migration de lá gravava esses literais
 * no banco. A grafia foi corrigida aqui, junto com a migration V21 — ainda não
 * havia dado em produção preso à grafia errada.
 */
public enum ResultValidation {

    /** Evidência enviada, aguardando a IA responder. */
    PENDING,

    /** A IA confirmou que a foto comprova a ação. É o que libera a recompensa. */
    APPROVED,

    /** A IA analisou e concluiu que a foto não comprova a ação. */
    REJECTED,

    /**
     * Não foi possível validar: a IA não respondeu, deu erro, ou a mensagem
     * esgotou as tentativas e caiu na DLQ.
     *
     * <p>Existe para separar falha de infraestrutura de reprovação legítima —
     * sem esse estado, as duas ficariam indistinguíveis, e um usuário que fez a
     * ação corretamente seria tratado como se tivesse mandado foto errada.
     */
    FAILED
}
