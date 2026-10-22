package br.com.fiap.soulie.infra.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contadores em memória, por instância.
 *
 * <p><b>Este é o modo em execução hoje</b>, não um fallback raro:
 * {@code soulie.ratelimit.distributed} é falso por padrão e o projeto ainda não
 * tem Redis provisionado. O limite não é compartilhado entre instâncias, então
 * com N instâncias o teto real é N vezes maior — mas continua barrando o abuso
 * óbvio, que é o ponto.
 *
 * <p><b>O mapa tem teto.</b> A versão anterior era um {@code ConcurrentHashMap}
 * sem limite, sem TTL e sem evicção, com a chave contendo o IP de quem chama:
 * cada origem distinta criava uma entrada que nunca saía, e isso era
 * {@code OutOfMemoryError} no próprio limitador — com o agravante de que o
 * reinício zera todos os contadores. O comentário de antes dizia que "o modo
 * degradado é temporário por definição"; não é, e por isso o teto existe.
 *
 * <p>O teto é um LRU: ao encher, a entrada menos usada recentemente sai. A troca
 * consciente é que, sob pressão de chaves, um contador legítimo pode ser
 * descartado antes da janela acabar. Isso é muito menos grave que derrubar o
 * processo, e ficou mais difícil de provocar depois que o IP de origem deixou de
 * ser falsificável por cabeçalho (ver {@code SocketAddressFilter}).
 */
@Component
public class InMemoryRateLimitStore implements RateLimitStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimitStore.class);

    /** Avisa uma vez que o teto foi atingido; é sinal de escalar ou ligar o Redis. */
    private final AtomicBoolean jaAvisouEvicao = new AtomicBoolean(false);

    private final Map<String, Bucket> baldes;

    public InMemoryRateLimitStore(RateLimitProperties properties) {
        int teto = Math.max(properties.getMaxInMemoryEntries(), 1_000);
        this.baldes = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Bucket> maisAntiga) {
                if (size() <= teto) {
                    return false;
                }
                if (jaAvisouEvicao.compareAndSet(false, true)) {
                    log.warn("Rate limit em memória atingiu o teto de {} contadores e começou a "
                            + "descartar os mais antigos. Considere ligar o Redis "
                            + "(soulie.ratelimit.distributed=true).", teto);
                }
                return true;
            }
        };
    }

    @Override
    public Resultado tentarConsumir(String chave, long capacidade, Duration janela) {
        String chaveDoBalde = chave + "|" + capacidade + "|" + janela.toSeconds();

        Bucket balde;
        // LinkedHashMap com ordem de acesso NÃO é thread-safe nem para leitura:
        // um get reordena a lista encadeada. Por isso o acesso é sincronizado, e
        // só ele — o consumo do token fica fora do lock, porque o Bucket do
        // Bucket4j já é thread-safe por conta própria.
        synchronized (baldes) {
            balde = baldes.computeIfAbsent(chaveDoBalde, ignorado -> Bucket.builder()
                    .addLimit(Bandwidth.builder()
                            .capacity(capacidade)
                            .refillIntervally(capacidade, janela)
                            .build())
                    .build());
        }

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
