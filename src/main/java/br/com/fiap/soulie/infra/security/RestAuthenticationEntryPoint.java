package br.com.fiap.soulie.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Responde 401 quando a requisição chega sem autenticação válida.
 *
 * <p>Existe para dar ao {@link SecurityFilter} um lugar único onde encerrar a
 * requisição quando o token é inválido — antes isso virava 500, porque exceção
 * de filtro não passa pelo {@code @RestControllerAdvice}.
 *
 * <p>Duas decisões de implementação:
 *
 * <ul>
 *   <li><b>Texto puro, não JSON.</b> O {@code ErrorHandler} já responde 401 com
 *       texto ({@code "Credenciais inválidas"}, {@code "Falha na autenticação"}),
 *       e é isso que o app consome hoje. Devolver JSON aqui criaria dois
 *       formatos de 401 na mesma API.</li>
 *   <li><b>Sem {@code ObjectMapper}.</b> A classe do Jackson 2 só está no
 *       classpath como dependência transitiva do {@code java-jwt}; a
 *       serialização da aplicação no Spring Boot 4.1 é Jackson 3. Depender dela
 *       compilaria hoje e quebraria no dia que o {@code java-jwt} mudasse.</li>
 * </ul>
 *
 * <p>A mensagem é sempre a mesma, independente da causa. Não dizemos se o token
 * está expirado, malformado, de outro emissor, ou se a conta foi removida ou
 * desativada: distinguir esses casos transformaria o 401 num oráculo para quem
 * está sondando quais contas existem.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String MENSAGEM = "Falha na autenticação";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        byte[] corpo = MENSAGEM.getBytes(StandardCharsets.UTF_8);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(corpo.length);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.getOutputStream().write(corpo);
    }
}
