package br.com.fiap.soulie.infra.storage;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * Guarda as imagens de evidência.
 *
 * <p>A chave do objeto é **sempre gerada pelo servidor**. Antes, o
 * {@code MissionApiService} guardava {@code photo.getOriginalFilename()} — um
 * valor que vem do cliente — e descartava os bytes. Usar o nome do cliente como
 * caminho abre path traversal e deixa um usuário sobrescrever o arquivo de
 * outro.
 */
public interface ImageStorage {

    /**
     * Sobe a imagem e devolve onde ela ficou.
     *
     * @throws ImageStorageException se o arquivo não for uma imagem aceita ou se
     *         o upload falhar
     */
    StoredImage store(MultipartFile file, String prefixo);

    /**
     * URL temporária de leitura, para a IA externa buscar a imagem.
     *
     * <p>É assim que uma IA de terceiro lê um objeto de um bucket **privado**
     * sem receber credencial nenhuma, e o acesso expira sozinho. A alternativa
     * seria deixar o bucket público — mas foto de evidência costuma mostrar a
     * casa e a rotina da pessoa.
     */
    String presignedReadUrl(String key, Duration validade);

    /** Busca os bytes, para quando a IA precisar receber o arquivo em vez da URL. */
    byte[] read(String key);
}
