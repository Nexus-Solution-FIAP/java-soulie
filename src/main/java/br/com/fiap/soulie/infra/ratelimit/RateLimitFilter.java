package br.com.fiap.soulie.infra.ratelimit;

import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.infra.cloudflare.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aplica os limites de uso.
 *
 * <p>Roda <b>antes</b> do {@code SecurityFilter}, para que {@code POST /login} e
 * {@code POST /user} — que são públicos — já cheguem protegidos. Isso tem uma
 * consequência: nessas rotas ainda não há usuário autenticado, então a chave é
 * o IP. Nas rotas autenticadas a chave é o id do usuário, que é mais justo e não
 * pune quem compartilha saída de rede.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final AntPathMatcher matcher = new AntPathMatcher();

    /** Evita encher o log com um alerta por requisição quando o Redis cai. */
    private final AtomicBoolean jaAvisouDegradacao = new AtomicBoolean(false);

    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final InMemoryRateLimitStore memoria;
    private final ObjectProvider<RedisRateLimitStore> redis;

    public RateLimitFilter(RateLimitProperties properties,
                           ClientIpResolver clientIpResolver,
                           InMemoryRateLimitStore memoria,
                           ObjectProvider<RedisRateLimitStore> redis) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.memoria = memoria;
        this.redis = redis;
    }

    /**
     * O caminho da requisicao, de forma confiavel.
     *
     * <p>Nao usa {@code getServletPath()}: ele vem vazio em varios arranjos
     * (inclusive no MockMvc, o que escondeu este filtro de um teste inteiro) e
     * muda se alguem definir {@code spring.mvc.servlet.path}. O URI menos o
     * context path e estavel nos dois mundos.
     */
    private String caminhoDe(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        if (contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)) {
            return uri.substring(contexto.length());
        }
        return uri;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = caminhoDe(request);
        // Swagger e /error não entram na conta: são navegação, não uso da API.
        return matcher.match("/v3/api-docs/**", path)
                || matcher.match("/swagger-ui/**", path)
                || "/swagger-ui.html".equals(path)
                || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var regra = regraPara(request);
        String chave = chaveDe(request, regra);

        RateLimitStore store = escolherStore();
        RateLimitStore.Resultado resultado;
        try {
            resultado = store.tentarConsumir(
                    chave + "|" + nomeDaRegra(request, regra),
                    regra == null ? properties.getDefaultCapacity() : regra.getCapacity(),
                    regra == null ? properties.getDefaultWindow() : regra.getWindow());
        } catch (Exception exception) {
            // Falha do próprio limitador não pode virar indisponibilidade da
            // API: libera e registra. Proteção que derruba o serviço é pior que
            // a ausência dela.
            log.error("Falha ao aplicar rate limit; liberando a requisição", exception);
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(resultado.restantes()));

        if (!resultado.permitido()) {
            responder429(response, resultado.esperarPor());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Redis quando disponível; memória quando não.
     *
     * <p>A degradação é deliberada: o projeto ainda não tem Redis provisionado,
     * e derrubar a API por causa disso seria desproporcional. O limite deixa de
     * ser compartilhado entre instâncias, mas continua barrando o abuso óbvio.
     */
    private RateLimitStore escolherStore() {
        RedisRateLimitStore distribuido = redis.getIfAvailable();
        if (distribuido != null && distribuido.disponivel()) {
            jaAvisouDegradacao.set(false);
            return distribuido;
        }
        if (distribuido != null && jaAvisouDegradacao.compareAndSet(false, true)) {
            log.warn("Redis indisponível: rate limit degradado para memória, por instância");
        }
        return memoria;
    }

    private RateLimitProperties.Regra regraPara(HttpServletRequest request) {
        String path = caminhoDe(request);
        String metodo = request.getMethod();
        return properties.getRules().stream()
                .filter(regra -> regra.getMethod() == null || regra.getMethod().equalsIgnoreCase(metodo))
                .filter(regra -> matcher.match(regra.getPath(), path))
                .findFirst()
                .orElse(null);
    }

    /**
     * Id do usuário quando autenticado; IP real quando não.
     *
     * <p>Uma regra pode forçar o IP ({@code byIp}) — é o caso do login e do
     * cadastro, onde ainda não existe usuário.
     */
    private String chaveDe(HttpServletRequest request, RateLimitProperties.Regra regra) {
        boolean forcarIp = regra != null && regra.isByIp();

        if (!forcarIp) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user && user.getId() != null) {
                return "u:" + user.getId();
            }
        }
        return "ip:" + clientIpResolver.resolver(request);
    }

    private String nomeDaRegra(HttpServletRequest request, RateLimitProperties.Regra regra) {
        return regra == null ? "default" : regra.getMethod() + ":" + regra.getPath();
    }

    private void responder429(HttpServletResponse response, Duration esperarPor) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        long segundos = Math.max(esperarPor.toSeconds(), 1);
        byte[] corpo = ("Muitas requisições. Tente novamente em " + segundos + " segundos.")
                .getBytes(StandardCharsets.UTF_8);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(corpo.length);
        response.setHeader("Retry-After", String.valueOf(segundos));
        response.getOutputStream().write(corpo);
    }
}
