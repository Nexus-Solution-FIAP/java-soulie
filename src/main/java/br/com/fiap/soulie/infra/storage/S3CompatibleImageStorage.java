package br.com.fiap.soulie.infra.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Guarda as imagens num bucket compatível com S3.
 *
 * <p>Funciona com o object storage do Railway, MinIO, Cloudflare R2 e a própria
 * AWS, porque o que muda entre eles é só o endpoint e a região. Duas opções são
 * necessárias para endpoint próprio:
 *
 * <ul>
 *   <li><b>path-style access</b> — o padrão do SDK é virtual-host
 *       ({@code bucket.endpoint}), que exige DNS por bucket e não funciona na
 *       maioria dos compatíveis;</li>
 *   <li><b>região literal</b> — o Railway usa {@code auto}, como o R2, que não é
 *       uma região da AWS.</li>
 * </ul>
 */
@Component
public class S3CompatibleImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(S3CompatibleImageStorage.class);

    private final ImageStorageProperties properties;
    private final S3Client client;
    private final S3Presigner presigner;

    public S3CompatibleImageStorage(ImageStorageProperties properties) {
        this.properties = properties;

        var credenciais = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
        var regiao = Region.of(properties.getRegion());
        var endpoint = URI.create(properties.getEndpoint());
        var configuracao = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        this.client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(regiao)
                .credentialsProvider(credenciais)
                .serviceConfiguration(configuracao)
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(regiao)
                .credentialsProvider(credenciais)
                .serviceConfiguration(configuracao)
                .build();
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

        // O tipo vem dos MAGIC BYTES, nunca do cabeçalho enviado pelo cliente.
        var tipo = ImageType.detectar(conteudo)
                .orElseThrow(() -> new ImageStorageException(
                        "O arquivo enviado não é uma imagem válida (aceitos: JPEG, PNG, GIF, WEBP)"));

        // Chave gerada pelo servidor. Nunca file.getOriginalFilename(): aquele
        // valor vem do cliente e permitiria path traversal e sobrescrever a foto
        // de outra pessoa. A data no caminho é só para o bucket ficar navegável.
        String key = "%s/%s/%s%s".formatted(prefixo, LocalDate.now(), UUID.randomUUID(), tipo.extension());

        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .contentType(tipo.contentType())
                            .contentLength((long) conteudo.length)
                            .build(),
                    RequestBody.fromBytes(conteudo));
        } catch (S3Exception exception) {
            // A mensagem do SDK pode trazer detalhe do bucket e da conta: fica no
            // log do servidor, não na resposta.
            log.error("Falha ao enviar imagem para o bucket", exception);
            throw new ImageStorageException("Não foi possível guardar a imagem agora");
        }

        return new StoredImage(key, urlPublica(key), tipo.contentType(), conteudo.length);
    }

    @Override
    public String presignedReadUrl(String key, Duration validade) {
        var pedido = GetObjectPresignRequest.builder()
                .signatureDuration(validade == null ? properties.getPresignedUrlTtl() : validade)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .build())
                .build();
        return presigner.presignGetObject(pedido).url().toString();
    }

    @Override
    public byte[] read(String key) {
        try {
            return client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build()).asByteArray();
        } catch (S3Exception exception) {
            log.error("Falha ao ler imagem do bucket", exception);
            throw new ImageStorageException("Não foi possível ler a imagem");
        }
    }

    /** Nulo quando o bucket é privado: aí a leitura é só por URL assinada. */
    private String urlPublica(String key) {
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return null;
        }
        return base.endsWith("/") ? base + key : base + "/" + key;
    }
}
