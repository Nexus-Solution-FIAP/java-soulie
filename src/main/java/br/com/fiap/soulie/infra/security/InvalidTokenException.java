package br.com.fiap.soulie.infra.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Token JWT ausente, malformado, expirado ou de outro emissor.
 *
 * <p>Estende {@code AuthenticationException} de propósito: é o que permite ao
 * {@link SecurityFilter} responder 401. Antes isto era um {@code RuntimeException}
 * cru lançado dentro do filtro — e exceção de filtro não passa pelo
 * {@code @RestControllerAdvice}, então o cliente recebia 500.
 */
public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
