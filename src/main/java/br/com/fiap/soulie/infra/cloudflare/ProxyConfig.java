package br.com.fiap.soulie.infra.cloudflare;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Faz o Spring entender que esta atras de um proxy.
 *
 * <p>Sem isto, a aplicacao monta URL absoluta a partir do que ELA ve — host e
 * esquema internos —, entao o cabecalho `Location` de um 201 e os links do
 * Swagger apontariam para `http://` e para o endereco interno do container, em
 * vez do dominio publico em https.
 *
 * <p>Precisa rodar bem no inicio da cadeia, antes de qualquer coisa que leia a
 * URL da requisicao.
 */
@Configuration
public class ProxyConfig {

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        var registro = new FilterRegistrationBean<>(new ForwardedHeaderFilter());
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }
}
