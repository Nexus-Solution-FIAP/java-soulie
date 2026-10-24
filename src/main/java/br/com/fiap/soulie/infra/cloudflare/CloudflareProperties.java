package br.com.fiap.soulie.infra.cloudflare;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuração do proxy do Cloudflare na frente da API.
 */
@ConfigurationProperties(prefix = "soulie.cloudflare")
public class CloudflareProperties {

    /**
     * Liga a confiança no cabeçalho {@code CF-Connecting-IP}.
     *
     * <p>Desligado por padrão, e isso é deliberado: ligar sem estar de fato
     * atrás do Cloudflare transforma o cabeçalho num campo livre que qualquer
     * cliente preenche.
     */
    private boolean enabled = false;

    /**
     * Faixas de onde o cabeçalho é aceito. Vazio usa a lista embutida.
     *
     * <p>As faixas do Cloudflare mudam de tempos em tempos
     * ({@code https://www.cloudflare.com/ips/}). Por isso são sobrescrevíveis
     * sem redeploy — a lista embutida é ponto de partida, não verdade eterna.
     */
    private List<String> trustedIps = List.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<String> getTrustedIps() { return trustedIps; }
    public void setTrustedIps(List<String> trustedIps) { this.trustedIps = trustedIps; }
}
