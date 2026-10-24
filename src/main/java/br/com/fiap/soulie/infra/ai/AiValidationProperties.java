package br.com.fiap.soulie.infra.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração da API externa que valida as imagens.
 *
 * <p>A URL vem **só daqui**, nunca de entrada do usuário. O worker faz uma
 * requisição HTTP de saída para o endereço configurado; se esse endereço
 * pudesse ser influenciado por quem envia a evidência, o worker viraria um
 * proxy de SSRF contra a rede interna.
 */
@ConfigurationProperties(prefix = "soulie.ai")
public class AiValidationProperties {

    /** Endpoint da API do parceiro que recebe a imagem e devolve o veredito. */
    private String url;

    /** Chave de API, enviada no cabeçalho Authorization. */
    private String apiKey;

    /**
     * Segredo compartilhado do webhook de retorno (HMAC-SHA256).
     *
     * <p>Obrigatório se o webhook estiver habilitado: ele concede XP, então sem
     * assinatura qualquer pessoa aprovaria a própria evidência.
     */
    private String webhookSecret;

    /** Liga o endpoint de callback. Desligado por padrão. */
    private boolean webhookEnabled = false;

    /**
     * Janela de tolerância do timestamp do webhook, contra replay. Uma
     * requisição assinada e capturada não pode valer para sempre.
     */
    private Duration webhookTolerance = Duration.ofMinutes(5);

    /** Como a imagem chega até a IA. */
    private DeliveryMode deliveryMode = DeliveryMode.PRESIGNED_URL;

    private Duration connectTimeout = Duration.ofSeconds(45);

    private Duration readTimeout = Duration.ofSeconds(90);

    /**
     * Quanto o {@code POST /validate} espera o veredito antes de devolver 202.
     * {@code PT0S} desliga a espera. Padrão 20s: no Render a IA costuma passar
     * dos 6s no cold start.
     */
    private Duration waitTimeout = Duration.ofSeconds(20);

    /**
     * Sem URL de IA, aprova a foto localmente. Só para {@code dev}: em
     * produção isso reabriria o bypass de XP.
     */
    private boolean stubApprove = false;


    public enum DeliveryMode {
        /** Manda uma URL temporária; funciona com bucket privado. */
        PRESIGNED_URL,
        /** Manda os bytes da imagem na própria requisição. */
        BYTES
    }


    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public boolean isWebhookEnabled() { return webhookEnabled; }
    public void setWebhookEnabled(boolean webhookEnabled) { this.webhookEnabled = webhookEnabled; }

    public Duration getWebhookTolerance() { return webhookTolerance; }
    public void setWebhookTolerance(Duration webhookTolerance) { this.webhookTolerance = webhookTolerance; }

    public DeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(DeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }

    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }

    public Duration getWaitTimeout() { return waitTimeout; }
    public void setWaitTimeout(Duration waitTimeout) { this.waitTimeout = waitTimeout; }

    public boolean isStubApprove() { return stubApprove; }
    public void setStubApprove(boolean stubApprove) { this.stubApprove = stubApprove; }

}
