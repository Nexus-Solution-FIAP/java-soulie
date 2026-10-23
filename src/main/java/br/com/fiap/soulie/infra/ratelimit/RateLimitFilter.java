package br.com.fiap.soulie.infra.ratelimit;

import br.com.fiap.soulie.infra.cloudflare.ClientIpResolver;
import br.com.fiap.soulie.infra.security.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aplica os limites de uso.
 *
 * <p>Roda <b>antes</b> do {@code SecurityFilter}, para que {@code POST /login} e
 * {@code POST /user} — que são públicos — já cheguem protegidos.
 *
 * <p>Isso cria um problema que a primeira versão deste filtro não resolveu:
 * rodando antes da autenticação, o {@code SecurityContextHolder} está
 * <b>sempre vazio</b> aqui (a API é {@code STATELESS}, não há sessão de onde o
 * contexto viesse), então ler o principal dali era código morto e <b>toda</b>
 * regra acabava chaveada por IP. O comentário antigo afirmava o contrário.
 *
 * <p>A correção é {@link #chaveDe}: o filtro lê o <i>subject</i> do próprio
 * token, sem autenticar e sem tocar no banco. Com isso a chave de uma rota
 * autenticada é a identidade, que é mais justa e não pune quem compartilha
 * saída de rede — e quem rotaciona IP não ganha balde novo.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final AntPathMatcher matcher = new AntPathMatcher();

    /** Evita encher o log com um alerta por requisição quando o Redis cai. */
    private final AtomicBoolean jaAvisouDegradacao = new AtomicBoolean(false);

    private static final String PREFIXO_BEARER = "Bearer ";

    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final TokenService tokenService;
    private final InMemoryRateLimitStore memoria;
    private final ObjectProvider<RedisRateLimitStore> redis;

    public RateLimitFilter(RateLimitProperties properties,
                           ClientIpResolver clientIpResolver,
                           TokenService tokenService,
                           InMemoryRateLimitStore memoria,
                           ObjectProvider<RedisRateLimitStore> redis) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.tokenService = tokenService;
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
        // Só /error fica de fora, porque é despacho interno do container: uma
        // requisição já contada não pode consumir um segundo token ao ser
        // encaminhada para a página de erro.
        //
        // O Swagger ESTAVA isento aqui, e isso era um buraco: /v3/api-docs é
        // público (permitAll) e gera o documento OpenAPI inteiro, então sem teto
        // era amplificação barata — uma requisição, resposta grande — além de
        // entregar o mapa completo da API. Agora ele cai no teto geral.
        return "/error".equals(caminhoDe(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var regra = regraPara(request);

        RateLimitStore.Resultado resultado;
        try {
            // chaveDe entra no try de proposito: ele resolve IP e token, e
            // excecao aqui escapava do filtro, virava 500 do container e nao
            // passava pelo @RestControllerAdvice.
            String chave = chaveDe(request, regra);
            RateLimitStore store = escolherStore();
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
     * Identidade de quem chamou quando há token válido; IP real quando não.
     *
     * <p>Uma regra pode forçar o IP ({@code byIp}) — é o caso do login e do
     * cadastro, onde ainda não existe usuário.
     *
     * <p>A identidade sai do <i>subject</i> do token, verificado aqui mesmo.
     * Não dá para ler o {@code SecurityContextHolder}: este filtro roda
     * <b>antes</b> do {@code SecurityFilter}, de propósito, então o contexto
     * ainda está vazio. Verificar a assinatura duas vezes custa um HMAC, que é
     * barato, e evita a alternativa ruim — consultar o banco antes de
     * autenticar.
     *
     * <p>O subject é um e-mail, então ele vai <b>em hash</b> para a chave. Chave
     * de contador acaba em log de Redis e em ferramenta de observação, e e-mail
     * de pessoa real não tem por que aparecer ali. O hash também fixa o tamanho
     * da chave, em vez de deixá-lo vir de entrada do cliente.
     */
    private String chaveDe(HttpServletRequest request, RateLimitProperties.Regra regra) {
        boolean forcarIp = regra != null && regra.isByIp();

        if (!forcarIp) {
            String identidade = identidadeDoToken(request);
            if (identidade != null) {
                return "u:" + identidade;
            }
        }
        return "ip:" + clientIpResolver.resolver(request);
    }

    /**
     * O subject do token, em hash — ou {@code null} se não houver token válido.
     *
     * <p>Token ausente, malformado, expirado ou assinado com outro segredo cai
     * para o limite por IP. Isso é o certo: sem identidade comprovada, o único
     * identificador honesto é de onde a conexão veio.
     */
    private String identidadeDoToken(HttpServletRequest request) {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER)) {
            return null;
        }
        try {
            String subject = tokenService.getSubject(cabecalho.substring(PREFIXO_BEARER.length()).trim());
            if (subject == null || subject.isBlank()) {
                return null;
            }
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(subject.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception exception) {
            return null;
        }
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
