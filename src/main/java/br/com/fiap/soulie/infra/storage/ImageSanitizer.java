package br.com.fiap.soulie.infra.storage;

import java.io.ByteArrayOutputStream;

/**
 * Reescreve a imagem a partir da estrutura dela, descartando o que não é pixel.
 *
 * <p>Resolve dois problemas de uma vez, e por isso é uma reconstrução e não dois
 * filtros:
 *
 * <ul>
 *   <li><b>Metadados.</b> A foto ia para o bucket exatamente como chegou, e daí
 *       seguia por URL assinada para a IA do parceiro — um terceiro. Foto de
 *       missão ambiental é tirada em casa ou no trajeto, e câmera de celular
 *       grava {@code GPSLatitude}/{@code GPSLongitude}, horário e modelo do
 *       aparelho no EXIF. Entregar a localização de quem tirou a foto não é
 *       parte do serviço.</li>
 *   <li><b>Polyglot.</b> A detecção de tipo olha os primeiros bytes, então um
 *       JPEG válido com qualquer coisa colada no fim
 *       ({@code cat ok.jpg payload > x.jpg}) era aceito e armazenado inteiro.
 *       Reconstruindo o arquivo segmento por segmento, o que vem depois do fim
 *       declarado simplesmente não é copiado.</li>
 * </ul>
 *
 * <p><b>Por que não reencodar com {@code ImageIO}.</b> Seria mais curto e
 * também apagaria tudo, mas o {@code ImageIO} de série no Java 21 não tem
 * decodificador de WebP — então WebP falharia ou teria de ser recusado — e o
 * reencode de JPEG é com perda, degradando justamente a evidência que a IA vai
 * analisar. Copiar os segmentos preserva os pixels bit a bit.
 *
 * <p>Se a estrutura não fizer sentido, o método devolve vazio e o upload é
 * recusado. Falhar fechado aqui é barato: o arquivo ou é uma imagem bem formada,
 * ou não precisa entrar.
 */
final class ImageSanitizer {

    private ImageSanitizer() {
    }

    /**
     * A imagem sem metadados e sem cauda, ou {@code null} se a estrutura for
     * inválida.
     */
    static byte[] limpar(byte[] conteudo, ImageType tipo) {
        try {
            return switch (tipo) {
                case JPEG -> limparJpeg(conteudo);
                case PNG -> limparPng(conteudo);
                case WEBP -> limparWebp(conteudo);
            };
        } catch (RuntimeException exception) {
            // Índice fora de faixa, tamanho declarado absurdo: é arquivo
            // malformado, e o lado seguro é recusar.
            return null;
        }
    }

    // ------------------------------------------------------------------ JPEG

    /** Marcadores sem campo de tamanho: não têm payload para pular. */
    private static boolean marcadorSolitario(int marcador) {
        return marcador == 0x01 || (marcador >= 0xD0 && marcador <= 0xD7);
    }

    /**
     * Mantém o que descreve a imagem e descarta o que descreve a <i>foto</i>.
     *
     * <p>Fora: {@code APP1} (onde vive o EXIF, com o GPS), os outros
     * {@code APPn} (XMP, Photoshop, ICC de origem) e {@code COM}. Dentro:
     * {@code APP0}/JFIF, tabelas de quantização e de Huffman, o quadro e os
     * dados comprimidos.
     */
    private static byte[] limparJpeg(byte[] c) {
        var saida = new ByteArrayOutputStream(c.length);
        saida.write(0xFF);
        saida.write(0xD8);

        int i = 2;
        while (i + 1 < c.length) {
            if ((c[i] & 0xFF) != 0xFF) {
                // Fora de sincronia com a sequência de marcadores.
                return null;
            }
            int marcador = c[i + 1] & 0xFF;

            if (marcador == 0xD9) {
                saida.write(0xFF);
                saida.write(0xD9);
                return saida.toByteArray();
            }
            if (marcadorSolitario(marcador)) {
                i += 2;
                continue;
            }
            if (i + 3 >= c.length) {
                return null;
            }
            int tamanho = ((c[i + 2] & 0xFF) << 8) | (c[i + 3] & 0xFF);
            if (tamanho < 2 || i + 2 + tamanho > c.length) {
                return null;
            }

            if (marcador == 0xDA) {
                // Start of Scan: o cabeçalho tem tamanho, mas os dados
                // comprimidos que vêm depois não. Copia daqui até o EOI e
                // encerra — o que estiver além do EOI é a cauda que queremos
                // perder.
                int fim = procurarEoi(c, i + 2 + tamanho);
                if (fim < 0) {
                    return null;
                }
                saida.write(c, i, fim - i);
                saida.write(0xFF);
                saida.write(0xD9);
                return saida.toByteArray();
            }

            boolean descartar = (marcador >= 0xE1 && marcador <= 0xEF) || marcador == 0xFE;
            if (!descartar) {
                saida.write(c, i, 2 + tamanho);
            }
            i += 2 + tamanho;
        }
        return null;
    }

    /** Posição do {@code FFD9}, pulando o {@code FF00} que é byte escapado. */
    private static int procurarEoi(byte[] c, int de) {
        for (int i = de; i + 1 < c.length; i++) {
            if ((c[i] & 0xFF) != 0xFF) {
                continue;
            }
            int proximo = c[i + 1] & 0xFF;
            if (proximo == 0xD9) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------- PNG

    /**
     * Pedaços mantidos: os críticos mais os que afetam como a imagem é exibida.
     *
     * <p>Ficam fora {@code eXIf} (o EXIF do PNG), {@code tEXt}/{@code zTXt}/
     * {@code iTXt} (texto arbitrário, onde ferramentas gravam de software a
     * coordenada) e {@code tIME}.
     */
    private static boolean pedacoPngPermitido(String tipo) {
        return switch (tipo) {
            case "IHDR", "PLTE", "IDAT", "IEND",
                 "tRNS", "gAMA", "cHRM", "sRGB", "iCCP", "bKGD", "pHYs", "sBIT" -> true;
            default -> false;
        };
    }

    private static byte[] limparPng(byte[] c) {
        var saida = new ByteArrayOutputStream(c.length);
        saida.write(c, 0, 8);

        int i = 8;
        while (i + 8 <= c.length) {
            int tamanho = lerInt32(c, i);
            if (tamanho < 0 || i + 12 + (long) tamanho > c.length) {
                return null;
            }
            String tipo = new String(c, i + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
            int total = 12 + tamanho;

            if (pedacoPngPermitido(tipo)) {
                saida.write(c, i, total);
            }
            if ("IEND".equals(tipo)) {
                // Fim declarado: o que vier depois não entra.
                return saida.toByteArray();
            }
            i += total;
        }
        return null;
    }

    // ------------------------------------------------------------------ WEBP

    private static byte[] limparWebp(byte[] c) {
        var corpo = new ByteArrayOutputStream(c.length);

        int i = 12; // "RIFF" + tamanho + "WEBP"
        while (i + 8 <= c.length) {
            String fourcc = new String(c, i, 4, java.nio.charset.StandardCharsets.US_ASCII);
            int tamanho = lerInt32LittleEndian(c, i + 4);
            if (tamanho < 0) {
                return null;
            }
            // Pedaço de tamanho ímpar leva um byte de alinhamento.
            int total = 8 + tamanho + (tamanho % 2);
            if (i + total > c.length) {
                // Último pedaço sem o byte de alinhamento acontece na prática.
                total = 8 + tamanho;
                if (i + total > c.length) {
                    return null;
                }
            }

            if ("EXIF".equals(fourcc) || "XMP ".equals(fourcc)) {
                i += total;
                continue;
            }
            if ("VP8X".equals(fourcc) && tamanho >= 1) {
                // O VP8X anuncia por bitflag que o arquivo tem EXIF (bit 3) e
                // XMP (bit 2). Removidos os pedaços, as flags têm de cair
                // também, senão o leitor procura o que não existe mais.
                byte[] copia = new byte[total];
                System.arraycopy(c, i, copia, 0, total);
                copia[8] = (byte) (copia[8] & ~0x0C);
                corpo.write(copia, 0, total);
                i += total;
                continue;
            }
            corpo.write(c, i, total);
            i += total;
        }

        byte[] pedacos = corpo.toByteArray();
        if (pedacos.length == 0) {
            return null;
        }
        var saida = new ByteArrayOutputStream(pedacos.length + 12);
        saida.write('R'); saida.write('I'); saida.write('F'); saida.write('F');
        // O tamanho do RIFF conta "WEBP" mais os pedaços, e tem de ser reescrito
        // porque pedaços saíram.
        escreverInt32LittleEndian(saida, 4 + pedacos.length);
        saida.write('W'); saida.write('E'); saida.write('B'); saida.write('P');
        saida.write(pedacos, 0, pedacos.length);
        return saida.toByteArray();
    }

    // ----------------------------------------------------------------- bytes

    private static int lerInt32(byte[] c, int de) {
        return ((c[de] & 0xFF) << 24) | ((c[de + 1] & 0xFF) << 16)
                | ((c[de + 2] & 0xFF) << 8) | (c[de + 3] & 0xFF);
    }

    private static int lerInt32LittleEndian(byte[] c, int de) {
        return (c[de] & 0xFF) | ((c[de + 1] & 0xFF) << 8)
                | ((c[de + 2] & 0xFF) << 16) | ((c[de + 3] & 0xFF) << 24);
    }

    private static void escreverInt32LittleEndian(ByteArrayOutputStream saida, int valor) {
        saida.write(valor & 0xFF);
        saida.write((valor >>> 8) & 0xFF);
        saida.write((valor >>> 16) & 0xFF);
        saida.write((valor >>> 24) & 0xFF);
    }
}
