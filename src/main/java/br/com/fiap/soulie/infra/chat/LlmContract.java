package br.com.fiap.soulie.infra.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Contrato interno com qualquer provedor OpenAI-compatível. Não é o /v1 do app. */
public final class LlmContract {

    private LlmContract() {
    }

    public record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            Double temperature,
            @JsonProperty("max_tokens") Integer maxTokens
    ) {
    }

    public record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatCompletionResponse(List<Choice> choices) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Choice(ChatMessage message) {
        }
    }
}
