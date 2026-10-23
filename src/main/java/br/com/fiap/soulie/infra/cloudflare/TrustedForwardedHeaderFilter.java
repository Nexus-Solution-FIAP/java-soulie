package br.com.fiap.soulie.infra.cloudflare;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * O {@code ForwardedHeaderFilter}, mas só para quem vem pelo proxy.
 *
 * <p>O filtro do Spring reescreve esquema, host e {@code getRemoteAddr()} a
 * partir de {@code X-Forwarded-*} e {@code Forwarded} <b>sem conferir de onde a
 * requisição veio</b>. Registrado sem condição, como estava, ele dava duas
 * coisas de graça a qualquer cliente:
 *
 * <ul>
 *   <li><b>o IP de origem</b>, que é o que o rate limit usa para decidir em quem
 *       confiar — bypass total do limite;</li>
 *   <li><b>o host das URLs absolutas</b> que a aplicação gera. Com
 *       {@code X-Forwarded-Host} livre, o {@code Location} de um 201 e os links
 *       do Swagger podem apontar para um domínio escolhido por quem chamou.</li>
 * </ul>
 *
 * <p>Com a condição, os cabeçalhos só valem quando a conexão <b>de verdade</b>
 * vem de faixa confiável, e a razão original de existir — montar URL com o
 * domínio público em https, em vez do endereço interno do contêiner — continua
 * valendo. Com {@code soulie.cloudflare.enabled=false} ele nunca embrulha: sem
 * proxy declarado, não há motivo para acreditar em cabeçalho de proxy.
 */
public class TrustedForwardedHeaderFilter extends ForwardedHeaderFilter {

    private final ClientIpResolver resolver;

    public TrustedForwardedHeaderFilter(ClientIpResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (super.shouldNotFilter(request)) {
            return true;
        }
        // Aqui a requisição ainda não foi embrulhada por ninguém, então
        // getRemoteAddr() é o endereço real da conexão.
        return !resolver.vemDeProxyConfiavel(request.getRemoteAddr());
    }
}
