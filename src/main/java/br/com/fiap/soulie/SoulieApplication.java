package br.com.fiap.soulie;

import br.com.fiap.soulie.infra.ai.AiValidationProperties;
import br.com.fiap.soulie.infra.cloudflare.CloudflareProperties;
import br.com.fiap.soulie.infra.ratelimit.RateLimitProperties;
import br.com.fiap.soulie.infra.storage.ImageStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        ImageStorageProperties.class,
        AiValidationProperties.class,
        CloudflareProperties.class,
        RateLimitProperties.class
})
public class SoulieApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoulieApplication.class, args);
    }

}
