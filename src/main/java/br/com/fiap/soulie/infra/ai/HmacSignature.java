package br.com.fiap.soulie.infra.ai;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Assinatura HMAC-SHA256 do webhook de validação.
 *
 * <p>O endpoint de callback concede XP. Sem assinatura, qualquer pessoa que
 * descobrisse a URL aprovaria a própria evidência — é o endpoint mais sensível
 * do projeto.
 *
 * <p>O que é assinado é {@code timestamp + "." + corpo}, não só o corpo. Incluir
 * o timestamp é o que permite recusar uma requisição legítima **capturada e
 * reenviada depois**: sem ele, a assinatura vale para sempre e um replay credita
 * XP de novo.
 */
@Component
public class HmacSignature {

    private static final String ALGORITMO = "HmacSHA256";

    /** Calcula a assinatura esperada, em hexadecimal minúsculo. */
    public String calcular(String segredo, String timestamp, String corpo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            byte[] assinatura = mac.doFinal((timestamp + "." + corpo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(assinatura);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível calcular a assinatura HMAC", exception);
        }
    }

    /**
     * Confere a assinatura recebida.
     *
     * <p>A comparação é feita com {@link MessageDigest#isEqual}, que percorre
     * todos os bytes independentemente de onde houver diferença. {@code equals}
     * de {@code String} retorna no primeiro byte divergente, e esse tempo
     * diferente permite descobrir a assinatura correta byte a byte.
     */
    public boolean confere(String segredo, String timestamp, String corpo, String assinaturaRecebida) {
        if (assinaturaRecebida == null || assinaturaRecebida.isBlank()) {
            return false;
        }
        String esperada = calcular(segredo, timestamp, corpo);
        return MessageDigest.isEqual(
                esperada.getBytes(StandardCharsets.UTF_8),
                assinaturaRecebida.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Recusa timestamp fora da janela de tolerância.
     *
     * <p>A janela vale para os dois lados: no futuro também, porque relógio
     * adiantado no emissor não deve virar uma assinatura válida por horas.
     */
    public boolean timestampValido(String timestamp, Duration tolerancia) {
        try {
            Instant enviado = Instant.ofEpochSecond(Long.parseLong(timestamp));
            Duration diferenca = Duration.between(enviado, Instant.now()).abs();
            return diferenca.compareTo(tolerancia) <= 0;
        } catch (NumberFormatException | NullPointerException exception) {
            return false;
        }
    }
}
