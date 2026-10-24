package br.com.fiap.soulie.infra.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuracao do bucket compativel com S3.
 *
 * <p>Os valores de producao apontam para o object storage do Railway
 * (endpoint t3.storageapi.dev, regiao "auto"). O mesmo codigo serve MinIO,
 * Cloudflare R2 e a AWS, trocando so as variaveis de ambiente.
 */
@ConfigurationProperties(prefix = "soulie.storage")
public class ImageStorageProperties {

    /**
     * {@code local} grava em disco (perfil dev). {@code s3} usa o bucket.
     * Sem valor, assume local para a API subir sem credencial de object storage.
     */
    private String type = "local";

    /** Pasta das evidências quando {@code type=local}. */
    private String localDir = "data/soulie-images";

    /** Endpoint do servico. Ex.: https://t3.storageapi.dev */
    private String endpoint;

    /** Regiao. O Railway usa "auto", como o R2. */
    private String region = "auto";

    private String bucket;

    private String accessKey;

    private String secretKey;

    /**
     * Base publica para montar a URL da imagem, quando o bucket servir leitura
     * publica. Vazio significa bucket privado: a leitura so acontece por URL
     * assinada.
     */
    private String publicBaseUrl;

    /**
     * Validade da URL assinada entregue a IA. Curta de proposito: e tempo de
     * uma chamada, nao de uma sessao.
     */
    private Duration presignedUrlTtl = Duration.ofMinutes(15);

    /** Tamanho maximo aceito. Espelha o limite de multipart do Spring. */
    private long maxBytes = 10L * 1024 * 1024;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocalDir() { return localDir; }
    public void setLocalDir(String localDir) { this.localDir = localDir; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

    public Duration getPresignedUrlTtl() { return presignedUrlTtl; }
    public void setPresignedUrlTtl(Duration presignedUrlTtl) { this.presignedUrlTtl = presignedUrlTtl; }

    public long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }
}
