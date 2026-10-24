package br.com.fiap.soulie.infra.storage;

/** Falha ao guardar ou ler uma imagem. */
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message) {
        super(message);
    }

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
