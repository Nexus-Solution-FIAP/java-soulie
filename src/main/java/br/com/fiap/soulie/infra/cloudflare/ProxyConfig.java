package br.com.fiap.soulie.infra.cloudflare;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Faz o Spring entender que esta atras de um proxy — sem acreditar em qualquer um.
 *
 * <p>Sem isto, a aplicacao monta URL absoluta a partir do que ELA ve — host e
 * esquema internos —, entao o cabecalho {@code Location} de um 201 e os links do
 * Swagger apontariam para {@code http://} e para o endereco interno do
 * container, em vez do dominio publico em https.
 *
 * <p>A ordem dos dois filtros e parte da correcao, nao detalhe:
 *
 * <ol>
 *   <li>o {@link SocketAddressFilter} roda primeiro e guarda o endereco real da
 *       conexao, antes de qualquer embrulho;</li>
 *   <li>o {@link TrustedForwardedHeaderFilter} vem depois e so reescreve a
 *       requisicao quando essa conexao vem de faixa confiavel.</li>
 * </ol>
 *
 * <p>A versao anterior registrava o {@code ForwardedHeaderFilter} cru em
 * {@code HIGHEST_PRECEDENCE}, e por isso o rate limit inteiro era contornavel
 * com um {@code X-Forwarded-For} — o motivo esta escrito em
 * {@link SocketAddressFilter}.
 */
@Configuration
public class ProxyConfig {

    @Bean
    public FilterRegistrationBean<SocketAddressFilter> socketAddressFilter() {
        var registro = new FilterRegistrationBean<>(new SocketAddressFilter());
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<TrustedForwardedHeaderFilter> forwardedHeaderFilter(
            ClientIpResolver resolver) {
        var registro = new FilterRegistrationBean<>(new TrustedForwardedHeaderFilter(resolver));
        // Depois do SocketAddressFilter, e ainda antes de qualquer coisa que leia
        // a URL da requisicao.
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registro;
    }
}
