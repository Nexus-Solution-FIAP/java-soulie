package br.com.fiap.soulie.infra.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Guarda evidências em disco. Só entra quando {@code soulie.storage.type=local},
 * o caminho do perfil {@code dev}: a API sobe sem bucket S3.
 */
@Component
@ConditionalOnProperty(name = "soulie.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

    private final ImageStorageProperties properties;
    private final Path root;

    public LocalImageStorage(ImageStorageProperties properties) {
        this.properties = properties;
        String dir = properties.getLocalDir() == null || properties.getLocalDir().isBlank()
                ? "data/soulie-images"
                : properties.getLocalDir();
        this.root = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new ImageStorageException("Não foi possível criar o diretório local de imagens", exception);
        }
        log.info("Storage de imagens em disco: {}", this.root);
    }

    @Override
    public StoredImage store(MultipartFile file, String prefixo) {
        if (file == null || file.isEmpty()) {
            throw new ImageStorageException("Nenhuma imagem foi enviada");
        }
        if (file.getSize() > properties.getMaxBytes()) {
            throw new ImageStorageException("A imagem excede o tamanho máximo permitido");
        }

        byte[] conteudo;
        try {
            conteudo = file.getBytes();
        } catch (IOException exception) {
            throw new ImageStorageException("Não foi possível ler a imagem enviada", exception);
        }

        var tipo = ImageType.detectar(conteudo)
                .orElseThrow(() -> new ImageStorageException(
                        "O arquivo enviado não é uma imagem válida (aceitos: JPEG, PNG, WEBP)"));
        conteudo = ImageSanitizer.limpar(conteudo, tipo);
        if (conteudo == null) {
            throw new ImageStorageException("A imagem enviada está corrompida ou tem formato inesperado");
        }

        String key = "%s/%s/%s%s".formatted(prefixo, LocalDate.now(), UUID.randomUUID(), tipo.extension());
        Path destino = resolver(key);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo);
        } catch (IOException exception) {
            throw new ImageStorageException("Não foi possível guardar a imagem agora", exception);
        }

        return new StoredImage(key, destino.toUri().toString(), tipo.contentType(), conteudo.length);
    }

    @Override
    public String presignedReadUrl(String key, Duration validade) {
        return resolver(key).toUri().toString();
    }

    @Override
    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolver(key));
        } catch (IOException exception) {
            throw new ImageStorageException("Não foi possível ler a imagem", exception);
        }
    }

    @Override
    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolver(key));
        } catch (IOException exception) {
            log.error("Não foi possível remover o arquivo local {}", key, exception);
        }
    }

    private Path resolver(String key) {
        Path destino = root.resolve(key).normalize();
        if (!destino.startsWith(root)) {
            throw new ImageStorageException("Chave de imagem inválida");
        }
        return destino;
    }
}
