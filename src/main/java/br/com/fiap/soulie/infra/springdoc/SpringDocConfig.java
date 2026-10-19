package br.com.fiap.soulie.infra.springdoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .servers(List.of(new Server().url("http://localhost:8080").description("API local")))
                .info(new Info()
                        .title("SoulUp - Soulie API")
                        .description("""
                                API do Soulie. O app usa o prefixo `/v1`. O mesmo contrato também responde em `/api/v1`.
                                
                                Fluxo: `POST /user` cria a conta, `POST /login` devolve `tokenJWT`, e as rotas de perfil, Soulie, missões, conquistas, comunidade e chat pedem `Authorization: Bearer`.
                                
                                A primeira ação do dia (regar ou concluir uma missão) conta 1 dia na sequência. No mesmo dia o número não sobe de novo.
                                """)
                        .contact(new Contact()
                                .name("Time Backend Nexus Solutions")
                                .email("backend@nexus.solutions.fiap"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://nexus-solutions.soulie/api/license")));
    }
}
