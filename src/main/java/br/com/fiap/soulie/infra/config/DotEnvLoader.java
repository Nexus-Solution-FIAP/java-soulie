package br.com.fiap.soulie.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Carrega {@code .env} da raiz do processo em system properties, sem sobrescrever
 * o que o SO ou o run config já definiram.
 *
 * <p>O Spring não lê {@code .env} sozinho. Sem isto, {@code SECRET_JWT} e a
 * chave do LLM só existem se alguém colar no IntelliJ — e um restart sem isso
 * desloga todo mundo ou cai o chat no FAQ sem ninguém perceber o motivo.
 */
public final class DotEnvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotEnvLoader.class);

    private DotEnvLoader() {
    }

    public static void load() {
        Path file = Path.of(".env");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int aplicadas = 0;
            for (String line : lines) {
                if (aplicar(line)) {
                    aplicadas++;
                }
            }
            log.info("Arquivo .env carregado ({} variáveis novas)", aplicadas);
        } catch (Exception exception) {
            log.warn("Não foi possível ler o .env: {}", exception.getClass().getSimpleName());
        }
    }

    static boolean aplicar(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.strip();
        if (trimmed.startsWith("\uFEFF")) {
            trimmed = trimmed.substring(1).strip();
        }
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return false;
        }
        int eq = trimmed.indexOf('=');
        if (eq <= 0) {
            return false;
        }
        String key = trimmed.substring(0, eq).strip();
        String value = destapar(trimmed.substring(eq + 1).strip());
        if (key.isEmpty()) {
            return false;
        }
        if (preenchido(System.getenv(key)) || preenchido(System.getProperty(key))) {
            return false;
        }
        System.setProperty(key, value);
        return true;
    }

    private static String destapar(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static boolean preenchido(String value) {
        return value != null && !value.isBlank();
    }
}
