package br.com.fiap.soulie.infra.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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
@ConditionalOnProperty(name = "soulie.storage.type", havingValue = "s3")
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
                // Timeouts explícitos, como manda a regra da casa para chamada
                // externa: o default do SDK vale para connect e read, mas sem
                // apiCallTimeout os 3 retries internos somam e uma requisição
                // pode segurar recurso por perto de um minuto e meio.
                .httpClient(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5))
                        .socketTimeout(Duration.ofSeconds(20))
                        .build())
                .overrideConfiguration(o -> o
                        .apiCallTimeout(Duration.ofSeconds(30))
                        .apiCallAttemptTimeout(Duration.ofSeconds(20)))
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
                        "O arquivo enviado não é uma imagem válida (aceitos: JPEG, PNG, WEBP)"));

        // Reescreve a imagem a partir da estrutura dela. Isto remove o EXIF --
        // onde a câmera grava GPS, horário e modelo do aparelho -- e descarta
        // qualquer coisa colada depois do fim declarado do arquivo. Os dois
        // importam porque este arquivo sai do nosso controle: vai para a IA do
        // parceiro por URL assinada.
        conteudo = ImageSanitizer.limpar(conteudo, tipo);
        if (conteudo == null) {
            throw new ImageStorageException(
                    "A imagem enviada está corrompida ou tem formato inesperado");
        }

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
        } catch (SdkException exception) {
            // SdkException e nao S3Exception: DNS, timeout de conexao e falha de
            // TLS sao SdkClientException, que NAO e S3Exception. Capturando so a
            // segunda, bucket inacessivel virava 500 com correlationId -- e a
            // documentacao prometia 400. A mensagem do SDK pode trazer detalhe do
            // bucket e da conta, entao fica no log, nao na resposta.
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
        } catch (SdkException exception) {
            log.error("Falha ao ler imagem do bucket", exception);
            throw new ImageStorageException("Não foi possível ler a imagem");
        }
    }

    @Override
    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (SdkException exception) {
            // So log: quem chama esta tratando um erro anterior, e trocar a
            // excecao dele pela desta esconderia a causa real. O objeto fica
            // orfao e a regra de ciclo de vida do bucket cuida dele.
            log.error("Nao foi possivel remover o objeto orfao {}", key, exception);
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
