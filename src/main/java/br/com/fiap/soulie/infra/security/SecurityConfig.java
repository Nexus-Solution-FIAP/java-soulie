package br.com.fiap.soulie.infra.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Origens autorizadas a chamar a API pelo navegador.
     *
     * <p>Antes aqui havia {@code setAllowedOriginPatterns(List.of("*"))} com todos
     * os headers liberados. Passou a ser uma lista explícita, vinda do ambiente:
     * em produção só as origens reais do app e da web.
     */
    @Value("${api.cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Recusa a subida da aplicação com CORS mal configurado.
     *
     * <p>Trocar o curinga por uma lista no código não bastava: a lista vem de
     * variável de ambiente, e {@code CorsConfiguration.setAllowedOrigins} aceita
     * {@code "*"} sem reclamar — com {@code allowCredentials = false} nem dispara
     * a validação que o proibiria. Ou seja, {@code CORS_ALLOWED_ORIGINS=*}
     * reintroduzia o problema inteiro sem tocar numa linha de código, e ninguém
     * perceberia.
     *
     * <p>Lista vazia também é recusada: ela não é uma brecha, mas faz todo
     * preflight responder 403 — indisponibilidade silenciosa do cliente web, que
     * é muito mais difícil de diagnosticar do que uma falha no boot.
     */
    @PostConstruct
    void validarCors() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()
                || allowedOrigins.stream().allMatch(String::isBlank)) {
            throw new IllegalStateException(
                    "CORS_ALLOWED_ORIGINS está vazia. Informe as origens do app, separadas por vírgula.");
        }
        if (allowedOrigins.stream().map(String::trim).anyMatch(origem -> origem.equals("*"))) {
            throw new IllegalStateException(
                    "CORS_ALLOWED_ORIGINS não aceita curinga. Liste as origens explicitamente.");
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // O próprio token Bearer já protege contra CSRF, e a API é stateless.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(req -> {
                    // Rotas públicas.
                    //
                    // PENDÊNCIA CONHECIDA: nenhuma delas tem rate limit hoje. Sem
                    // teto, POST /login é força bruta de senha e POST /user é
                    // criação em massa de conta — que é justamente o insumo de
                    // qualquer ataque que precise de "algum login válido".
                    // O limite entra na Fase 6, com Bucket4j e Redis. Até lá, o
                    // Cloudflare na frente é a única barreira.
                    req.requestMatchers(HttpMethod.POST, "/login", "/user").permitAll();
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();

                    // Callback da IA de validação. Não passa por JWT porque quem
                    // chama é um serviço, não um usuário — a autenticação dele é
                    // a assinatura HMAC conferida no próprio controller, com
                    // janela de timestamp contra replay.
                    //
                    // O controller só existe quando soulie.ai.webhook-enabled é
                    // true (@ConditionalOnProperty), então com o webhook
                    // desligado esta rota simplesmente não existe e responde 404.
                    req.requestMatchers(HttpMethod.POST, "/webhooks/validations").permitAll();

                    // Escrita no catalogo compartilhado de acoes e operacao
                    // administrativa. Antes era aberta a qualquer autenticado: dava
                    // para injetar conteudo no catalogo de todos e, pior, definir
                    // quantos pontos a acao vale — com `points` grande o bastante,
                    // concluir a missao causaria overflow de int no XP e no saldo.
                    req.requestMatchers(HttpMethod.POST, "/action").hasRole("ADMIN");

                    req.anyRequest().authenticated();
                })
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Location", "Retry-After", "X-RateLimit-Remaining"));
        // A autenticação é por header Bearer, não por cookie, então não há motivo
        // para permitir credenciais — e permitir impediria origem curinga.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
