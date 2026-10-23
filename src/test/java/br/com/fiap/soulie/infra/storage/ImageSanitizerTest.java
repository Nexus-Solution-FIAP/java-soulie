package br.com.fiap.soulie.infra.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O saneamento da imagem: sem metadados e sem cauda.
 *
 * <p>Os dois problemas que ele resolve eram reais. A foto ia para o bucket
 * exatamente como chegou e seguia por URL assinada para a IA do parceiro — um
 * terceiro —, levando o EXIF, onde a câmera grava {@code GPSLatitude} e
 * {@code GPSLongitude}. E a detecção de tipo olha só os primeiros bytes, então
 * um JPEG válido com qualquer coisa colada no fim era aceito e armazenado
 * inteiro.
 */
class ImageSanitizerTest {

    // ------------------------------------------------------------- fabricação

    private static void marcadorComTamanho(ByteArrayOutputStream saida, int marcador, byte[] dados) {
        saida.write(0xFF);
        saida.write(marcador);
        int tamanho = dados.length + 2;
        saida.write((tamanho >>> 8) & 0xFF);
        saida.write(tamanho & 0xFF);
        saida.writeBytes(dados);
    }

    /**
     * Um JPEG mínimo mas estruturalmente honesto: SOI, APP0/JFIF, APP1 com um
     * EXIF de mentira, o scan e o EOI.
     */
    private static byte[] jpegComExif() {
        var saida = new ByteArrayOutputStream();
        saida.write(0xFF);
        saida.write(0xD8);
        marcadorComTamanho(saida, 0xE0, "JFIF\0".getBytes(StandardCharsets.US_ASCII));
        marcadorComTamanho(saida, 0xE1,
                "Exif\0\0GPSLatitude=-23.5505;GPSLongitude=-46.6333".getBytes(StandardCharsets.US_ASCII));
        marcadorComTamanho(saida, 0xDA, new byte[]{0x01, 0x02});
        saida.writeBytes(new byte[]{0x11, 0x22, 0x33});
        saida.write(0xFF);
        saida.write(0xD9);
        return saida.toByteArray();
    }

    private static void pedacoPng(ByteArrayOutputStream saida, String tipo, byte[] dados) {
        int tamanho = dados.length;
        saida.write((tamanho >>> 24) & 0xFF);
        saida.write((tamanho >>> 16) & 0xFF);
        saida.write((tamanho >>> 8) & 0xFF);
        saida.write(tamanho & 0xFF);
        saida.writeBytes(tipo.getBytes(StandardCharsets.US_ASCII));
        saida.writeBytes(dados);
        saida.writeBytes(new byte[]{0, 0, 0, 0}); // CRC de fachada
    }

    private static byte[] pngComTextoEExif() {
        var saida = new ByteArrayOutputStream();
        saida.writeBytes(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        pedacoPng(saida, "IHDR", new byte[13]);
        pedacoPng(saida, "eXIf", "GPSLatitude=-23.5505".getBytes(StandardCharsets.US_ASCII));
        pedacoPng(saida, "tEXt", "Comment\0tirada em casa".getBytes(StandardCharsets.US_ASCII));
        pedacoPng(saida, "IDAT", new byte[]{0x01, 0x02, 0x03});
        pedacoPng(saida, "IEND", new byte[0]);
        return saida.toByteArray();
    }

    private static void pedacoWebp(ByteArrayOutputStream saida, String fourcc, byte[] dados) {
        saida.writeBytes(fourcc.getBytes(StandardCharsets.US_ASCII));
        int tamanho = dados.length;
        saida.write(tamanho & 0xFF);
        saida.write((tamanho >>> 8) & 0xFF);
        saida.write((tamanho >>> 16) & 0xFF);
        saida.write((tamanho >>> 24) & 0xFF);
        saida.writeBytes(dados);
        if (tamanho % 2 == 1) {
            saida.write(0);
        }
    }

    private static byte[] webpComExif() {
        var pedacos = new ByteArrayOutputStream();
        // VP8X com as flags de EXIF (bit 3) e XMP (bit 2) ligadas.
        pedacoWebp(pedacos, "VP8X", new byte[]{0x0C, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        pedacoWebp(pedacos, "VP8 ", new byte[]{0x01, 0x02, 0x03, 0x04});
        pedacoWebp(pedacos, "EXIF", "GPSLatitude=-23.5505".getBytes(StandardCharsets.US_ASCII));

        byte[] corpo = pedacos.toByteArray();
        var saida = new ByteArrayOutputStream();
        saida.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        int tamanho = 4 + corpo.length;
        saida.write(tamanho & 0xFF);
        saida.write((tamanho >>> 8) & 0xFF);
        saida.write((tamanho >>> 16) & 0xFF);
        saida.write((tamanho >>> 24) & 0xFF);
        saida.writeBytes("WEBP".getBytes(StandardCharsets.US_ASCII));
        saida.writeBytes(corpo);
        return saida.toByteArray();
    }

    private static boolean contem(byte[] conteudo, String agulha) {
        return new String(conteudo, StandardCharsets.ISO_8859_1).contains(agulha);
    }

    // ------------------------------------------------------------------ casos

    @Test
    @DisplayName("JPEG: o EXIF com GPS sai, e os dados da imagem ficam")
    void jpegPerdeOExifEMantemAImagem() {
        byte[] original = jpegComExif();
        assertThat(contem(original, "GPSLatitude")).as("o fabricado tem GPS").isTrue();

        byte[] limpo = ImageSanitizer.limpar(original, ImageType.JPEG);

        assertThat(limpo).isNotNull();
        assertThat(contem(limpo, "GPSLatitude"))
                .as("a coordenada de quem tirou a foto não pode seguir para a IA")
                .isFalse();
        assertThat(contem(limpo, "JFIF")).as("o APP0 continua").isTrue();
        assertThat(limpo).as("continua sendo um JPEG").startsWith((byte) 0xFF, (byte) 0xD8);
        assertThat(limpo).hasSizeLessThan(original.length);
    }

    @Test
    @DisplayName("POLYGLOT: o que vem depois do fim do JPEG e descartado")
    void jpegPerdeACaudaColada() {
        // `cat ok.jpg payload > x.jpg`: a detecção de tipo olha os primeiros
        // bytes e aceitava o arquivo inteiro.
        var comCauda = new ByteArrayOutputStream();
        comCauda.writeBytes(jpegComExif());
        comCauda.writeBytes("<script>alert(1)</script>".getBytes(StandardCharsets.US_ASCII));
        comCauda.writeBytes(new byte[]{0x50, 0x4B, 0x03, 0x04}); // cabeçalho de ZIP

        byte[] limpo = ImageSanitizer.limpar(comCauda.toByteArray(), ImageType.JPEG);

        assertThat(limpo).isNotNull();
        assertThat(contem(limpo, "<script>")).isFalse();
        assertThat(contem(limpo, "PK")).isFalse();
        assertThat(limpo).endsWith((byte) 0xFF, (byte) 0xD9);
    }

    @Test
    @DisplayName("PNG: eXIf e tEXt saem, IHDR e IDAT ficam")
    void pngPerdeMetadados() {
        byte[] limpo = ImageSanitizer.limpar(pngComTextoEExif(), ImageType.PNG);

        assertThat(limpo).isNotNull();
        assertThat(contem(limpo, "GPSLatitude")).isFalse();
        assertThat(contem(limpo, "tirada em casa")).isFalse();
        assertThat(contem(limpo, "eXIf")).isFalse();
        assertThat(contem(limpo, "tEXt")).isFalse();
        assertThat(contem(limpo, "IHDR")).isTrue();
        assertThat(contem(limpo, "IDAT")).isTrue();
        assertThat(contem(limpo, "IEND")).isTrue();
    }

    @Test
    @DisplayName("POLYGLOT: o que vem depois do IEND e descartado")
    void pngPerdeACaudaColada() {
        var comCauda = new ByteArrayOutputStream();
        comCauda.writeBytes(pngComTextoEExif());
        comCauda.writeBytes("CARGA-ESCONDIDA".getBytes(StandardCharsets.US_ASCII));

        byte[] limpo = ImageSanitizer.limpar(comCauda.toByteArray(), ImageType.PNG);

        assertThat(limpo).isNotNull();
        assertThat(contem(limpo, "CARGA-ESCONDIDA")).isFalse();
    }

    @Test
    @DisplayName("WEBP: o pedaco EXIF sai e a flag do VP8X e desligada")
    void webpPerdeOExifEAFlag() {
        byte[] limpo = ImageSanitizer.limpar(webpComExif(), ImageType.WEBP);

        assertThat(limpo).isNotNull();
        assertThat(contem(limpo, "GPSLatitude")).isFalse();
        assertThat(contem(limpo, "EXIF")).isFalse();
        assertThat(contem(limpo, "VP8X")).as("o VP8X continua, só sem as flags").isTrue();

        // A flag precisa cair junto: mantida, um leitor procuraria um pedaço
        // que não existe mais.
        int inicioVp8x = new String(limpo, StandardCharsets.ISO_8859_1).indexOf("VP8X");
        assertThat(limpo[inicioVp8x + 8] & 0x0C)
                .as("bits de EXIF e XMP do VP8X têm de estar zerados")
                .isZero();

        // E o tamanho do RIFF tem de ser reescrito, já que pedaços saíram.
        int tamanhoRiff = (limpo[4] & 0xFF) | ((limpo[5] & 0xFF) << 8)
                | ((limpo[6] & 0xFF) << 16) | ((limpo[7] & 0xFF) << 24);
        assertThat(tamanhoRiff).isEqualTo(limpo.length - 8);
    }

    @Test
    @DisplayName("Estrutura invalida e recusada em vez de passar adiante")
    void estruturaInvalidaEhRecusada() {
        // Assinatura de JPEG com lixo atrás: a detecção de tipo aceita, o
        // saneamento não. Falhar fechado aqui é barato.
        byte[] soAssinatura = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x11, 0x22, 0x33,
                0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99};

        assertThat(ImageSanitizer.limpar(soAssinatura, ImageType.JPEG)).isNull();
        assertThat(ImageSanitizer.limpar(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A, 0x7F, 0x7F, 0x7F, 0x7F}, ImageType.PNG)).isNull();
    }

    @Test
    @DisplayName("Sanear duas vezes nao muda mais nada")
    void saneamentoEhIdempotente() {
        byte[] umaVez = ImageSanitizer.limpar(jpegComExif(), ImageType.JPEG);
        byte[] duasVezes = ImageSanitizer.limpar(umaVez, ImageType.JPEG);

        assertThat(duasVezes).isEqualTo(umaVez);
    }
}
