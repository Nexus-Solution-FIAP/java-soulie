package br.com.fiap.soulie.infra.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

/**
 * Contadores no Redis, compartilhados entre todas as instâncias.
 *
 * <p>Usa uma janela fixa implementada em Lua. O script roda <b>atomicamente</b>
 * dentro do Redis: incrementar e conferir em comandos separados seria uma
 * corrida, e duas instâncias passariam juntas pelo limite — exatamente o
 * problema que o modo distribuído existe para resolver.
 *
 * <p>Janela fixa, e não token bucket, de propósito: ela cabe em cinco linhas de
 * Lua auditáveis, enquanto um bucket distribuído exigiria guardar e interpolar
 * estado. A diferença prática é a rajada na virada da janela — aceitável para
 * limites de abuso, que é o uso aqui.
 *
 * <p>Se o Redis não responde, {@link #disponivel()} devolve falso e o
 * {@code RateLimitFilter} cai para o modo em memória. Nunca propaga a falha
 * para o usuário: o rate limit é proteção, não funcionalidade.
 */
public class RedisRateLimitStore implements RateLimitStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStore.class);

    /**
     * INCR + EXPIRE numa operação só.
     *
     * <p>O {@code EXPIRE} só é aplicado quando o contador acabou de nascer
     * ({@code == 1}); renová-lo a cada acesso transformaria a janela fixa numa
     * janela deslizante que nunca expira enquanto houver tráfego.
     */
    private static final String SCRIPT = """
            local atual = redis.call('INCR', KEYS[1])
            if atual == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('TTL', KEYS[1])
            return {atual, ttl}
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> script;

    public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptText(SCRIPT);
        this.script.setResultType(List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Resultado tentarConsumir(String chave, long capacidade, Duration janela) {
        List<Long> retorno = redisTemplate.execute(
                script,
                List.of("ratelimit:" + chave),
                String.valueOf(janela.toSeconds()));

        if (retorno == null || retorno.size() < 2) {
            // Resposta inesperada: libera em vez de barrar. O rate limit não
            // deve virar indisponibilidade por um erro dele mesmo.
            log.warn("Resposta inesperada do Redis no rate limit; liberando");
            return Resultado.liberado(capacidade);
        }

        long atual = retorno.get(0);
        long ttl = Math.max(retorno.get(1), 0);

        if (atual > capacidade) {
            return Resultado.bloqueado(Duration.ofSeconds(ttl));
        }
        return Resultado.liberado(capacidade - atual);
    }

    @Override
    public boolean disponivel() {
        try {
            redisTemplate.hasKey("ratelimit:ping");
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
