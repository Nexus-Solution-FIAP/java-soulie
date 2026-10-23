package br.com.fiap.soulie.infra.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Monta o armazenamento do rate limit conforme o que existe no ambiente.
 *
 * <p>O {@code RedisRateLimitStore} so vira bean quando ha um
 * {@code StringRedisTemplate} — ou seja, quando o Redis esta configurado. Sem
 * ele, sobra o {@code InMemoryRateLimitStore}, e o limite vale por instancia.
 * O projeto hoje nao tem Redis provisionado, entao esse e o caminho ativo.
 */
@Configuration
public class RateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    @Bean
    @ConditionalOnProperty(name = "soulie.ratelimit.distributed", havingValue = "true")
    public RedisRateLimitStore redisRateLimitStore(StringRedisTemplate redisTemplate) {
        log.info("Rate limit distribuido: usando Redis");
        return new RedisRateLimitStore(redisTemplate);
    }
}
