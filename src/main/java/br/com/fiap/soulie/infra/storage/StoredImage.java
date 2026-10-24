package br.com.fiap.soulie.infra.storage;

/**
 * Onde a imagem ficou guardada.
 *
 * @param key         chave do objeto no bucket, gerada pelo servidor (UUID)
 * @param url         URL publica de leitura, quando houver base publica configurada
 * @param contentType tipo detectado pelos MAGIC BYTES do arquivo, nao pelo
 *                    cabecalho que o cliente mandou
 * @param sizeBytes   tamanho em bytes
 */
public record StoredImage(String key, String url, String contentType, long sizeBytes) {
}
