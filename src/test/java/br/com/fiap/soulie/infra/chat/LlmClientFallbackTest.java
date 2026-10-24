package br.com.fiap.soulie.infra.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LlmClientFallbackTest {

    @Autowired
    private LlmClient llmClient;

    @Test
    @DisplayName("Sem URL configurada o client não chama a rede")
    void semUrlDevolveVazio() {
        assertThat(llmClient.complete("system", "user")).isEmpty();
    }
}
