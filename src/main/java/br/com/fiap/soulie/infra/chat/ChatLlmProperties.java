package br.com.fiap.soulie.infra.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do LLM da Alma.
 *
 * <p>A URL vem <b>só daqui</b>, nunca do body do app. O service faz uma
 * requisição de saída para este endereço; se viesse do cliente, viraria SSRF.
 *
 * <p>O payload é o contrato OpenAI {@code chat/completions}. Groq e o endpoint
 * compatível da Gemini falam a mesma língua — trocar provedor é YAML, não Java.
 * Sem URL ou sem chave, o {@link LlmClient} não chama ninguém e o chat cai no FAQ.
 */
@ConfigurationProperties(prefix = "soulie.chat")
public class ChatLlmProperties {

    private String url;

    private String apiKey;

    private String model = "qwen/qwen3.8-27b";

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(20);

    private int maxTokens = 600;

    private double temperature = 0.4;

    public boolean isConfigured() {
        return url != null && !url.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}
