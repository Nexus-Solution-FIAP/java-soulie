package br.com.fiap.soulie.infra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration // Aqui identificamos que essa classe é de configuração ao Spring Boot
@EnableWebSecurity // Indica que vamos personalizar as configurações (Autenticação)
// @EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean // Não vai ser automaticamente, essa anotação serve para expor o retorno desse método
    // Assim eu posso devolver um Objeto para o Spring, podendo injetar onde necessário (outras classes)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        // CSRF - Cross-Site Request Forgery
        // O próprio Token é uma proteção contra esses ataques
        return http.csrf(csrf ->
                        csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> { // Aqui eu digo para o Spring permitir a requisição
                    // ...apenas do método POST e solicitar autenticação de todo o resto
                    req.requestMatchers(HttpMethod.POST,"/login", "/user").permitAll();
                    req.anyRequest().authenticated();
                }).addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class) // Estou informando para
                // o Spring que eu quero que meu filtro(SecurityFilter) seja feito antes do filtro dele(UsernamePass...)
                .build();
        // Aqui eu desabilito o formulário stateful e agora ativo o stateless - Porque estamos usando API Rest
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
