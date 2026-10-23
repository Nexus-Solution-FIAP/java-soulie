package br.com.fiap.soulie.infra.security;

import br.com.fiap.soulie.domain.user.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final static String ISSUER = "SoulUp.Soulie";

    /** HMAC-SHA256 trabalha com blocos de 32 bytes; abaixo disso a chave perde força. */
    private final static int MINIMO_DE_CARACTERES_DO_SEGREDO = 32;

    private final static Duration EXPIRACAO_MINIMA = Duration.ofMinutes(5);
    private final static Duration EXPIRACAO_MAXIMA = Duration.ofHours(24);

    @Value("${api.security.token.secret_jwt}")
    private String secretJWT;

    @Value("${api.security.token.expiration:PT2H}")
    private Duration expiration;

    /**
     * Recusa a subida da aplicação com configuração de token inválida.
     *
     * <p>Tirar o valor padrão do {@code SECRET_JWT} no YAML já faz o boot falhar
     * quando a variável **não existe**. Mas não cobria os dois casos que
     * acontecem de verdade:
     *
     * <ul>
     *   <li><b>vazia</b> — uma variável criada sem valor resolve para {@code ""}.
     *       A aplicação subia e só explodia na hora de assinar, porque
     *       {@code SecretKeySpec} rejeita chave de tamanho zero: todos os logins
     *       em erro, com mensagem confusa.</li>
     *   <li><b>curta</b> — aceita em silêncio. Uma chave de ~9 bytes é
     *       força-brutável offline a partir de um único token; quebrada, forja-se
     *       token para qualquer e-mail, o que é o mesmo impacto do
     *       {@code "SenhaReserva"} que acabamos de remover.</li>
     * </ul>
     *
     * <p>Vale o mesmo para a expiração: {@code JWT_EXPIRATION=7200} é lido pelo
     * Spring como 7200 <b>milissegundos</b> (token de 7,2 segundos), não 2 horas,
     * e converte sem reclamar. O piso e o teto pegam isso.
     */
    @PostConstruct
    void validarConfiguracao() {
        if (secretJWT == null || secretJWT.isBlank()) {
            throw new IllegalStateException(
                    "SECRET_JWT não pode ser vazia. Gere um valor com: openssl rand -base64 48");
        }
        if (secretJWT.length() < MINIMO_DE_CARACTERES_DO_SEGREDO) {
            throw new IllegalStateException(
                    "SECRET_JWT tem %d caracteres e precisa de no mínimo %d. Gere um valor com: openssl rand -base64 48"
                            .formatted(secretJWT.length(), MINIMO_DE_CARACTERES_DO_SEGREDO));
        }
        if (expiration == null || expiration.compareTo(EXPIRACAO_MINIMA) < 0
                || expiration.compareTo(EXPIRACAO_MAXIMA) > 0) {
            throw new IllegalStateException(
                    ("JWT_EXPIRATION fora da faixa aceita (%s a %s): %s. "
                            + "Use o formato ISO-8601 de duração, como PT2H — um número puro é lido como milissegundos.")
                            .formatted(EXPIRACAO_MINIMA, EXPIRACAO_MAXIMA, expiration));
        }
    }

    public String generateToken(User user) {
        try {
            var algorithm = Algorithm.HMAC256(secretJWT);
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.getUsername())
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(expiresIn())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new IllegalStateException("Erro ao gerar token JWT", exception);
        }
    }

    /**
     * Devolve o e-mail (subject) do token.
     *
     * @throws InvalidTokenException se o token for inválido, expirado ou de outro emissor.
     *         É uma {@code AuthenticationException}, então o {@link SecurityFilter}
     *         consegue responder 401 em vez de deixar escapar um 500.
     */
    public String getSubject(String tokenJWT) {
        try {
            var algorithm = Algorithm.HMAC256(secretJWT);
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new InvalidTokenException("Token JWT inválido ou expirado");
        }
    }

    // Instant.now() já é UTC. A versão anterior somava as horas em LocalDateTime
    // (hora local da máquina) e convertia com o offset fixo -03:00: num servidor
    // em UTC, como o do Railway, isso gerava token de 5 horas em vez de 2.
    private Instant expiresIn() {
        return Instant.now().plus(expiration);
    }
}
