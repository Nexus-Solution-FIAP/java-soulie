package br.com.fiap.soulie.infra.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Chama um LLM no contrato OpenAI {@code chat/completions}.
 *
 * <p>Molde de {@code ExpoPushService} / {@code ImageValidationClient}:
 * {@code RestClient} sobre o {@code HttpClient} do JDK, timeouts explícitos,
 * records do JSON, falha capturada. Sem URL ou chave, devolve vazio — o chat
 * cai no FAQ, a API não cai.
 *
 * <p>Não loga o prompt nem a chave. A mensagem do usuário é PII.
 */
@Service
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private static final Pattern THINK_BLOCK = Pattern.compile("(?is)<think>.*?</think>");

    private final ChatLlmProperties properties;
    private final RestClient client;

    public LlmClient(ChatLlmProperties properties) {
        this.properties = properties;
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.getReadTimeout());
        this.client = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Optional<String> complete(String system, String user) {
        if (!properties.isConfigured()) {
            return Optional.empty();
        }
        try {
            var body = new LlmContract.ChatCompletionRequest(
                    properties.getModel(),
                    List.of(
                            new LlmContract.ChatMessage("system", system),
                            new LlmContract.ChatMessage("user", user)
                    ),
                    properties.getTemperature(),
                    properties.getMaxTokens()
            );
            LlmContract.ChatCompletionResponse response = client.post()
                    .uri(properties.getUrl())
                    .headers(headers -> headers.setBearerAuth(properties.getApiKey()))
                    .body(body)
                    .retrieve()
                    .body(LlmContract.ChatCompletionResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("LLM respondeu sem choices");
                return Optional.empty();
            }
            var message = response.choices().getFirst().message();
            if (message == null || message.content() == null) {
                return Optional.empty();
            }
            String text = limpar(message.content());
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (Exception exception) {
            log.warn("Falha ao chamar o LLM: {}", exception.getClass().getSimpleName());
            log.debug("Detalhe da falha no LLM", exception);
            return Optional.empty();
        }
    }

    static String limpar(String content) {
        String sem = THINK_BLOCK.matcher(content).replaceAll("").strip();
        if (sem.contains("<think>")) {
            return "";
        }
        return sem;
    }
}
