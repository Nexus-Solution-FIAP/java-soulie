package br.com.fiap.soulie.infra.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvLoaderTest {

    private static final String CHAVE = "SOULIE_DOTENV_TEST_KEY";

    @AfterEach
    void limpar() {
        System.clearProperty(CHAVE);
    }

    @Test
    void aplicaLinhaNova() {
        assertThat(DotEnvLoader.aplicar(CHAVE + "=abc")).isTrue();
        assertThat(System.getProperty(CHAVE)).isEqualTo("abc");
    }

    @Test
    void naoSobrescrevePropertyJaDefinida() {
        System.setProperty(CHAVE, "original");
        assertThat(DotEnvLoader.aplicar(CHAVE + "=novo")).isFalse();
        assertThat(System.getProperty(CHAVE)).isEqualTo("original");
    }

    @Test
    void ignoraComentarioELinhaVazia() {
        assertThat(DotEnvLoader.aplicar("# segredo=x")).isFalse();
        assertThat(DotEnvLoader.aplicar("   ")).isFalse();
        assertThat(DotEnvLoader.aplicar(null)).isFalse();
    }
}
