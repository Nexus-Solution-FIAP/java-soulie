package br.com.fiap.soulie.infra.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O tipo do arquivo é decidido pelos bytes, não pelo que o cliente declara.
 */
class ImageTypeTest {

    private static byte[] comBytes(int... bytes) {
        // 16 bytes: a detecção exige pelo menos 12.
        byte[] conteudo = new byte[16];
        for (int i = 0; i < bytes.length; i++) {
            conteudo[i] = (byte) bytes[i];
        }
        return conteudo;
    }

    @Test
    void reconheceJpeg() {
        assertThat(ImageType.detectar(comBytes(0xFF, 0xD8, 0xFF))).contains(ImageType.JPEG);
    }

    @Test
    void reconhecePng() {
        assertThat(ImageType.detectar(comBytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
                .contains(ImageType.PNG);
    }

    @Test
    @DisplayName("RIFF só é WEBP se os bytes 8-11 disserem WEBP")
    void naoConfundeOutrosArquivosRiffComWebp() {
        // "RIFF" + 4 bytes de tamanho + "WAVE" = um WAV, não uma imagem.
        byte[] wav = comBytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x41, 0x56, 0x45);
        assertThat(ImageType.detectar(wav)).isEmpty();

        byte[] webp = comBytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50);
        assertThat(ImageType.detectar(webp)).contains(ImageType.WEBP);
    }

    @Test
    @DisplayName("Executável disfarçado de imagem é recusado")
    void recusaExecutavel() {
        // "MZ" é o cabeçalho de um .exe do Windows.
        assertThat(ImageType.detectar(comBytes(0x4D, 0x5A, 0x90, 0x00))).isEmpty();
    }

    @Test
    @DisplayName("SVG é recusado: é XML e pode conter script")
    void recusaSvg() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);
        assertThat(ImageType.detectar(svg)).isEmpty();
    }

    @Test
    @DisplayName("HTML com script é recusado, mesmo que o cliente diga image/jpeg")
    void recusaHtml() {
        byte[] html = "<html><script>fetch('/roubar')</script></html>".getBytes(StandardCharsets.UTF_8);
        assertThat(ImageType.detectar(html)).isEmpty();
    }

    @Test
    void recusaArquivoVazioOuCurtoDemais() {
        assertThat(ImageType.detectar(null)).isEmpty();
        assertThat(ImageType.detectar(new byte[0])).isEmpty();
        assertThat(ImageType.detectar(new byte[]{(byte) 0xFF, (byte) 0xD8})).isEmpty();
    }
}
