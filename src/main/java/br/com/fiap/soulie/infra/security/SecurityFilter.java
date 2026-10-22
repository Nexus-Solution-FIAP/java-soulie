package br.com.fiap.soulie.infra.security;

import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);
    private static final String PREFIXO_BEARER = "Bearer ";

    /**
     * Rotas que não passam por este filtro.
     *
     * <p>Sem isto, um app que anexa {@code Authorization} em toda chamada (o
     * padrão de um interceptor HTTP) ficava **travado fora da conta** quando o
     * token expirava: o filtro recusava o token obsoleto e devolvia 401 no
     * próprio {@code POST /login}, antes de a requisição chegar ao controller —
     * então não havia como renovar. O token velho no header precisa ser ignorado
     * aqui, não rejeitado.
     */
    private static final List<String> ROTAS_PUBLICAS = List.of(
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/error"
    );

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestAuthenticationEntryPoint entryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (ROTAS_PUBLICAS.stream().anyMatch(padrao -> matcher.match(padrao, path))) {
            return true;
        }
        // Cadastro e login são públicos: um token obsoleto no header não pode
        // impedir o usuário de se autenticar de novo.
        return HttpMethod.POST.matches(request.getMethod())
                && ("/login".equals(path) || "/user".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var tokenJWT = recoverToken(request);

        if (tokenJWT != null) {
            try {
                autenticar(tokenJWT);
            } catch (AuthenticationException exception) {
                // Exceção lançada aqui NÃO passa pelo @RestControllerAdvice, porque o
                // filtro roda antes do DispatcherServlet. Sem este tratamento o cliente
                // recebia 500 para um token simplesmente expirado.
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response, exception);
                return;
            } catch (RuntimeException exception) {
                // Qualquer outra falha — banco fora do ar no findByEmail, erro de
                // configuração do algoritmo — também escaparia do filtro e cairia
                // no /error do container, onde a mensagem crua pode vazar.
                var correlationId = UUID.randomUUID().toString();
                log.error("Falha inesperada ao autenticar [{}]", correlationId, exception);
                SecurityContextHolder.clearContext();
                entryPoint.commence(request, response,
                        new InvalidTokenException("Não foi possível validar a autenticação"));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(String tokenJWT) {
        var subject = tokenService.getSubject(tokenJWT);
        User user = userRepository.findByEmail(subject);

        // O token pode ser válido e o usuário não existir mais (conta removida do
        // banco). Antes isso estourava NullPointerException em getAuthorities().
        if (user == null) {
            log.debug("Token válido para um e-mail que não existe mais");
            throw new InvalidTokenException("Não foi possível validar a autenticação");
        }

        // Conta desativada (exclusão lógica) não continua autenticada só porque
        // ainda tem um token válido na mão.
        if (!user.isEnabled()) {
            log.debug("Token válido de conta desativada");
            throw new InvalidTokenException("Não foi possível validar a autenticação");
        }

        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String recoverToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIXO_BEARER)) {
            return null;
        }
        var token = authorizationHeader.substring(PREFIXO_BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
