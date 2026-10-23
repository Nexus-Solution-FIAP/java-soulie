package br.com.fiap.soulie.infra.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A assinatura do webhook é o que separa "a IA aprovou" de "qualquer um
 * aprovou": o endpoint concede XP e é público.
 */
class HmacSignatureTest {

    private static final String SEGREDO = "segredo-compartilhado-com-o-parceiro-de-equipe";

    private final HmacSignature hmac = new HmacSignature();

    private static String agora() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    @Test
    void aceitaAssinaturaCorreta() {
        String timestamp = agora();
        String corpo = "{\"validationId\":42,\"status\":\"APPROVED\"}";
        String assinatura = hmac.calcular(SEGREDO, timestamp, corpo);

        assertThat(hmac.confere(SEGREDO, timestamp, corpo, assinatura)).isTrue();
    }

    @Test
    void recusaAssinaturaDeOutroSegredo() {
        String timestamp = agora();
        String corpo = "{\"validationId\":42}";
        String assinaturaIntrusa = hmac.calcular("outro-segredo-qualquer", timestamp, corpo);

        assertThat(hmac.confere(SEGREDO, timestamp, corpo, assinaturaIntrusa)).isFalse();
    }

    @Test
    @DisplayName("Alterar o corpo invalida a assinatura")
    void recusaCorpoAlterado() {
        String timestamp = agora();
        String original = "{\"validationId\":42,\"status\":\"REJECTED\"}";
        String assinatura = hmac.calcular(SEGREDO, timestamp, original);

        String adulterado = "{\"validationId\":42,\"status\":\"APPROVED\"}";

        assertThat(hmac.confere(SEGREDO, timestamp, adulterado, assinatura)).isFalse();
    }

    @Test
    @DisplayName("Reusar a assinatura com outro timestamp não funciona")
    void assinaturaEstaPresaAoTimestamp() {
        String corpo = "{\"validationId\":42}";
        String assinatura = hmac.calcular(SEGREDO, "1700000000", corpo);

        assertThat(hmac.confere(SEGREDO, "1700000001", corpo, assinatura)).isFalse();
    }

    @Test
    void recusaAssinaturaAusente() {
        assertThat(hmac.confere(SEGREDO, agora(), "{}", null)).isFalse();
        assertThat(hmac.confere(SEGREDO, agora(), "{}", "")).isFalse();
    }

    @Test
    @DisplayName("Requisição capturada e reenviada depois é recusada pelo timestamp")
    void recusaReplay() {
        String antigo = String.valueOf(Instant.now().minus(Duration.ofHours(2)).getEpochSecond());

        assertThat(hmac.timestampValido(antigo, Duration.ofMinutes(5))).isFalse();
    }

    @Test
    @DisplayName("Timestamp no futuro também é recusado")
    void recusaTimestampAdiantado() {
        String futuro = String.valueOf(Instant.now().plus(Duration.ofHours(2)).getEpochSecond());

        assertThat(hmac.timestampValido(futuro, Duration.ofMinutes(5))).isFalse();
    }

    @Test
    void aceitaTimestampDentroDaJanela() {
        assertThat(hmac.timestampValido(agora(), Duration.ofMinutes(5))).isTrue();
    }

    @Test
    void recusaTimestampMalformado() {
        assertThat(hmac.timestampValido("ontem", Duration.ofMinutes(5))).isFalse();
        assertThat(hmac.timestampValido(null, Duration.ofMinutes(5))).isFalse();
    }
}
