package br.com.fiap.soulie.infra.storage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos de imagem aceitos, identificados pelos <b>magic bytes</b> do arquivo.
 *
 * <p>O {@code Content-Type} de um upload é informado pelo cliente e não custa
 * nada falsificar: dá para enviar um executável, um HTML com script ou um
 * arquivo gigante de ZIP declarando {@code image/jpeg}. Quem decide o que o
 * arquivo é são os primeiros bytes dele.
 *
 * <p>SVG fica deliberadamente de fora: é XML, pode conter script, e seria
 * armazenado e depois servido ao app.
 *
 * <p><b>GIF saiu</b> quando o {@link ImageSanitizer} entrou. Nenhuma câmera
 * produz GIF para comprovar uma ação, e manter o formato exigiria um quarto
 * parser de estrutura só para ele — ou deixá-lo como o único caminho que escapa
 * da remoção de metadados e da poda da cauda. Menos formato aceito é menos
 * superfície.
 */
public enum ImageType {

    JPEG("image/jpeg", ".jpg", new int[]{0xFF, 0xD8, 0xFF}),
    PNG("image/png", ".png", new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    // WEBP é um contêiner RIFF: "RIFF" nos bytes 0-3 e "WEBP" nos 8-11.
    WEBP("image/webp", ".webp", new int[]{0x52, 0x49, 0x46, 0x46});

    private final String contentType;
    private final String extension;
    private final int[] assinatura;

    ImageType(String contentType, String extension, int[] assinatura) {
        this.contentType = contentType;
        this.extension = extension;
        this.assinatura = assinatura;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    /**
     * Identifica o formato pelos bytes iniciais, ou devolve vazio se não for uma
     * imagem reconhecida.
     */
    public static Optional<ImageType> detectar(byte[] conteudo) {
        if (conteudo == null || conteudo.length < 12) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(tipo -> tipo.combina(conteudo))
                .findFirst();
    }

    private boolean combina(byte[] conteudo) {
        for (int i = 0; i < assinatura.length; i++) {
            if ((conteudo[i] & 0xFF) != assinatura[i]) {
                return false;
            }
        }
        // RIFF sozinho também é WAV e AVI: só é WEBP se os bytes 8-11 disserem.
        if (this == WEBP) {
            return (conteudo[8] & 0xFF) == 0x57   // W
                    && (conteudo[9] & 0xFF) == 0x45   // E
                    && (conteudo[10] & 0xFF) == 0x42  // B
                    && (conteudo[11] & 0xFF) == 0x50; // P
        }
        return true;
    }
}
