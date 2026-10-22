package br.com.fiap.soulie.infra.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contadores em memória, por instância.
 *
 * <p>É o modo degradado: vale quando não há Redis configurado ou quando ele está
 * fora do ar. O limite deixa de ser compartilhado entre instâncias, então com N
 * instâncias o teto real é N vezes maior — mas continua barrando o abuso óbvio,
 * que é o ponto.
 *
 * <p>O mapa cresce com as chaves vistas. Em escala isso precisaria de expiração
 * (Caffeine com TTL, por exemplo); aqui o modo degradado é temporário por
 * definição, e a troca é consciente.
 */
@Component
public class InMemoryRateLimitStore implements RateLimitStore {

    private final Map<String, Bucket> baldes = new ConcurrentHashMap<>();

    @Override
    public Resultado tentarConsumir(String chave, long capacidade, Duration janela) {
        Bucket balde = baldes.computeIfAbsent(
                chave + "|" + capacidade + "|" + janela.toSeconds(),
                ignorado -> Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacidade)
                                .refillIntervally(capacidade, janela)
                                .build())
                        .build());

        ConsumptionProbe sonda = balde.tryConsumeAndReturnRemaining(1);
        if (sonda.isConsumed()) {
            return Resultado.liberado(sonda.getRemainingTokens());
        }
        return Resultado.bloqueado(Duration.ofNanos(sonda.getNanosToWaitForRefill()));
    }

    @Override
    public boolean disponivel() {
        return true;
    }
}
