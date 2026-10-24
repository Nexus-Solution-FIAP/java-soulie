package br.com.fiap.soulie.infra.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmClientTest {

    @Test
    void removeBlocoThinkDoQwen() {
        String bruto = "<think>raciocínio interno</think>\nOlá, guardião.";
        assertThat(LlmClient.limpar(bruto)).isEqualTo("Olá, guardião.");
    }

    @Test
    void thinkTruncadoViraVazio() {
        assertThat(LlmClient.limpar("<think>ainda raciocinando")).isEmpty();
    }
}
