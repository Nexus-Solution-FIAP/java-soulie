package br.com.fiap.soulie.infra.ai;

import br.com.fiap.soulie.infra.storage.ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.Base64;
import java.util.Optional;

/**
 * Chama a API de validação de imagem do parceiro e lê o veredito.
 *
 * <p>Segue o molde de {@code infra/push/ExpoPushService}: {@code RestClient}
 * sobre o {@code HttpClient} do JDK, timeouts explícitos de connect e read, e
 * falha tratada em vez de propagada. É o único jeito de fazer chamada externa
 * neste projeto.
 *
 * <p>A chamada é **síncrona de propósito**, apesar de o fluxo ser assíncrono:
 * quem dá o assíncrono é a fila. Rodando dentro do worker, esperar a resposta é
 * simples e correto — e o timeout de leitura é o que impede um worker de ficar
 * preso para sempre numa IA que não responde.
 */
@Service
public class ImageValidationClient {

    private static final Logger log = LoggerFactory.getLogger(ImageValidationClient.class);

    private final AiValidationProperties properties;
    private final ImageStorage imageStorage;
    private final RestClient client;

    public ImageValidationClient(AiValidationProperties properties, ImageStorage imageStorage) {
        this.properties = properties;
        this.imageStorage = imageStorage;

        HttpClient http = HttpClient.newBuilder()
                // HTTP/1.1 de propósito: o JDK escolhe HTTP/2 por padrão, e o
                // uvicorn da IA (h11) responde 400 "Invalid HTTP request received"
                // para o preâmbulo h2c em http://localhost. HTTPS/ALPN do Expo
                // não entra aqui — esta chamada é clara, na rede local.
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.getConnectTimeout())
                .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.getReadTimeout());

        this.client = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Envia a imagem e devolve o veredito.
     *
     * @return o resultado, ou vazio se não foi possível obter resposta. Vazio
     *         significa <b>falha de análise</b>, não reprovação: quem chama tem
     *         de distinguir os dois, senão um usuário que fez a ação certa seria
     *         tratado como quem mandou foto errada.
     */
    public Optional<ImageValidationContract.Result> validate(Long validationId,
                                                             String imageKey,
                                                             String contentType,
                                                             ImageValidationContract.Mission mission,
                                                             String callbackUrl) {
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            if (properties.isStubApprove()) {
                log.warn("soulie.ai.url vazia: validação {} aprovada pelo stub de desenvolvimento", validationId);
                return Optional.of(new ImageValidationContract.Result(
                        "APPROVED",
                        java.util.List.of("Validação local (perfil dev)"),
                        1.0,
                        "Stub de desenvolvimento: sem API de IA configurada",
                        "stub-dev"));
            }
            log.warn("soulie.ai.url não configurada: validação {} não pode ser analisada", validationId);
            return Optional.empty();
        }

        try {
            // montarRequisicao DENTRO do try: ele gera a URL assinada, e para
            // evidencia do fluxo legado o imageKey e nulo -- o SDK lancava
            // "Parameter Key must not be null" e a excecao escapava deste
            // metodo, consumindo as 4 tentativas e indo para a DLQ sem nunca
            // chamar a IA. Dentro do try, isso virou um Optional vazio, que o
            // consumidor trata como falha de analise.
            var request = montarRequisicao(validationId, imageKey, contentType, mission, callbackUrl);

            var resultado = client.post()
                    .uri(properties.getUrl())
                    .headers(headers -> {
                        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                            headers.setBearerAuth(properties.getApiKey());
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(ImageValidationContract.Result.class);

            if (resultado == null || resultado.status() == null) {
                log.warn("IA respondeu sem status para a validação {}", validationId);
                return Optional.empty();
            }
            return Optional.of(resultado);
        } catch (Exception exception) {
            // Sem a mensagem da exceção no nível de erro: ela costuma trazer a URL
            // chamada, e com ela a URL assinada da foto de uma pessoa. A chave de
            // API vai no cabeçalho Authorization, nunca na URL. O detalhe fica no
            // debug.
            log.warn("Falha ao chamar a IA para a validação {}: {}",
                    validationId, exception.getClass().getSimpleName());
            log.debug("Detalhe da falha na validação {}", validationId, exception);
            return Optional.empty();
        }
    }

    private ImageValidationContract.Request montarRequisicao(Long validationId,
                                                             String imageKey,
                                                             String contentType,
                                                             ImageValidationContract.Mission mission,
                                                             String callbackUrl) {
        if (properties.getDeliveryMode() == AiValidationProperties.DeliveryMode.BYTES) {
            byte[] conteudo = imageStorage.read(imageKey);
            return new ImageValidationContract.Request(
                    validationId, null, Base64.getEncoder().encodeToString(conteudo),
                    contentType, mission, callbackUrl);
        }

        // Padrão: URL temporária. Funciona com bucket privado e não entrega
        // credencial nenhuma à ponta da IA. O prazo é o configurado em
        // soulie.storage.presigned-url-ttl (null = usa o padrão do storage).
        String url = imageStorage.presignedReadUrl(imageKey, null);
        return new ImageValidationContract.Request(
                validationId, url, null, contentType, mission, callbackUrl);
    }
}
