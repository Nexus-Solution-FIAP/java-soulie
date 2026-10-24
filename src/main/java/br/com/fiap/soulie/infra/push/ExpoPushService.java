package br.com.fiap.soulie.infra.push;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ExpoPushService {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushService.class);
    private static final String CHANNEL_ID = "soulie-jardim";

    private final RestClient client;

    public ExpoPushService() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder()
                .baseUrl("https://exp.host/--/api/v2")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Accept-Encoding", "gzip, deflate")
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(factory)
                .build();
    }

    public Result send(String token, String title, String body, Long notificationId) {
        if (token == null || token.isBlank()) {
            return Result.SKIPPED;
        }
        try {
            ExpoPushTicketResponse response = client.post()
                    .uri("/push/send")
                    .body(List.of(new ExpoPushMessage(
                            token,
                            title,
                            body,
                            "default",
                            CHANNEL_ID,
                            "high",
                            Map.of(
                                    "type", "nudge",
                                    "url", "/jardim",
                                    "id", notificationId == null ? "" : String.valueOf(notificationId)
                            )
                    )))
                    .retrieve()
                    .body(ExpoPushTicketResponse.class);
            if (response != null && response.data() != null) {
                for (ExpoPushTicket ticket : response.data()) {
                    if (ticket != null && ticket.details() != null
                            && "DeviceNotRegistered".equals(ticket.details().error())) {
                        return Result.UNREGISTERED;
                    }
                }
            }
            return Result.SENT;
        } catch (Exception exception) {
            log.warn("Não foi possível enviar o push Expo: {}", exception.getMessage());
            return Result.SKIPPED;
        }
    }

    public enum Result {
        SENT,
        SKIPPED,
        UNREGISTERED
    }

    record ExpoPushMessage(
            String to,
            String title,
            String body,
            String sound,
            String channelId,
            String priority,
            Map<String, String> data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoPushTicketResponse(List<ExpoPushTicket> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoPushTicket(String status, String message, ExpoPushDetails details) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExpoPushDetails(String error) {
    }
}
