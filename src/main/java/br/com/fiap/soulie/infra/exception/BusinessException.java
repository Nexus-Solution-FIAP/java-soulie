package br.com.fiap.soulie.infra.exception;

import org.springframework.http.HttpStatus;

/**
 * Regra de negócio violada, com o status HTTP que o caso merece.
 *
 * <p>No repositório {@code soulie-soul-up} esta classe existia vazia e nunca foi
 * usada; as regras lançavam {@code jakarta.validation.ValidationException}, que
 * sempre resulta em 400. Isso é certo para entrada inválida, mas errado quando o
 * problema é de outra natureza — tentar acessar o recurso de outra pessoa é 403,
 * e refazer uma missão já concluída no período é 409, não 400.
 *
 * <p>A mensagem é escrita para o usuário final, então o {@code ErrorHandler}
 * pode devolvê-la no corpo da resposta. Nunca coloque detalhe interno aqui.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** O recurso existe, mas não é do usuário logado. */
    public static BusinessException acessoNegado(String message) {
        return new BusinessException(message, HttpStatus.FORBIDDEN);
    }

    /** A operação conflita com o estado atual (já feito, duplicado). */
    public static BusinessException conflito(String message) {
        return new BusinessException(message, HttpStatus.CONFLICT);
    }

    /** Dependência externa indisponível (fila, storage, IA). */
    public static BusinessException indisponivel(String message) {
        return new BusinessException(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /** Cota ou limite de uso estourado. */
    public static BusinessException limiteExcedido(String message) {
        return new BusinessException(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
