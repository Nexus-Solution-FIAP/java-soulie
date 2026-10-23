package br.com.fiap.soulie.infra.ratelimit;

import java.time.Duration;

/**
 * Onde os contadores do rate limit vivem.
 *
 * <p>Existe como porta porque o Redis e OPCIONAL neste projeto: quando ele nao
 * esta configurado ou esta fora do ar, o limite passa a valer por instancia, em
 * memoria. Degradar assim e deliberado — derrubar a API inteira porque o Redis
 * caiu seria falha fechada demais, e deixar passar tudo seria falha aberta
 * demais.
 */
public interface RateLimitStore {

    /**
     * Tenta consumir um token do balde identificado por {@code chave}.
     *
     * @param capacidade quantas requisicoes cabem na janela
     * @param janela     duracao da janela
     * @return o resultado, com quantas restam e quanto esperar se estourou
     */
    Resultado tentarConsumir(String chave, long capacidade, Duration janela);

    /** O armazenamento esta respondendo? Usado para decidir a degradacao. */
    boolean disponivel();

    /**
     * @param permitido  se a requisicao pode seguir
     * @param restantes  quantas ainda cabem na janela
     * @param esperarPor quanto falta para liberar, quando estourou
     */
    record Resultado(boolean permitido, long restantes, Duration esperarPor) {

        public static Resultado liberado(long restantes) {
            return new Resultado(true, restantes, Duration.ZERO);
        }

        public static Resultado bloqueado(Duration esperarPor) {
            return new Resultado(false, 0, esperarPor);
        }
    }
}
