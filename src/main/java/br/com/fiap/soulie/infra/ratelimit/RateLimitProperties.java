package br.com.fiap.soulie.infra.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * As faixas de limite, em configuracao e nao em codigo.
 *
 * <p>Ficam aqui porque sao numeros de operacao: ajustar um limite nao deve
 * exigir recompilar.
 */
@ConfigurationProperties(prefix = "soulie.ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * Usa o Redis para compartilhar os contadores entre instancias.
     *
     * <p>Falso por padrao porque o projeto ainda nao tem Redis provisionado.
     * Com falso, o limite vale por instancia — continua barrando o abuso
     * obvio, mas com N instancias o teto real e N vezes maior.
     */
    private boolean distributed = false;

    /** Teto geral para quem esta autenticado e nao casa com regra especifica. */
    private long defaultCapacity = 120;

    private Duration defaultWindow = Duration.ofMinutes(1);

    private List<Regra> rules = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isDistributed() { return distributed; }
    public void setDistributed(boolean distributed) { this.distributed = distributed; }

    public long getDefaultCapacity() { return defaultCapacity; }
    public void setDefaultCapacity(long defaultCapacity) { this.defaultCapacity = defaultCapacity; }

    public Duration getDefaultWindow() { return defaultWindow; }
    public void setDefaultWindow(Duration defaultWindow) { this.defaultWindow = defaultWindow; }

    public List<Regra> getRules() { return rules; }
    public void setRules(List<Regra> rules) { this.rules = rules; }

    /**
     * Uma faixa.
     *
     * <p>{@code byIp} existe porque nem toda rota pode ser limitada por usuario:
     * no login e no cadastro nao HA usuario ainda, entao o unico identificador
     * e o IP.
     */
    public static class Regra {
        private String method;
        private String path;
        private long capacity;
        private Duration window;
        private boolean byIp = false;

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public long getCapacity() { return capacity; }
        public void setCapacity(long capacity) { this.capacity = capacity; }

        public Duration getWindow() { return window; }
        public void setWindow(Duration window) { this.window = window; }

        public boolean isByIp() { return byIp; }
        public void setByIp(boolean byIp) { this.byIp = byIp; }
    }
}
