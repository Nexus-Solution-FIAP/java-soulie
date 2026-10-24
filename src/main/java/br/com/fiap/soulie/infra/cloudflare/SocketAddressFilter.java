package br.com.fiap.soulie.infra.cloudflare;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guarda o endereço real da conexão, antes que alguém o reescreva.
 *
 * <p>Existe por causa de um bypass real deste projeto. O
 * {@code ForwardedHeaderFilter} <b>embrulha a requisição e sobrescreve
 * {@code getRemoteAddr()}</b> com o valor mais à esquerda de
 * {@code X-Forwarded-For} — que é um cabeçalho que o cliente escreve. Com ele
 * registrado em {@code HIGHEST_PRECEDENCE}, como estava, o "IP do socket" que o
 * {@link ClientIpResolver} usava para decidir em quem confiar era <b>escolhido
 * pelo cliente</b>, e o rate limit inteiro caía com um cabeçalho de uma linha:
 * um IP diferente por requisição, balde novo a cada chamada, nenhum 429.
 *
 * <p>Estar atrás do Cloudflare não ajudava: ele <b>acrescenta</b> à lista do
 * {@code X-Forwarded-For} em vez de substituí-la, então o valor mais à esquerda
 * continua sendo o que o atacante mandou.
 *
 * <p>Este filtro roda primeiro, antes de qualquer embrulho, e grava o endereço
 * verdadeiro num atributo da requisição. Quem precisa decidir confiança lê o
 * atributo, nunca {@code getRemoteAddr()}.
 */
public class SocketAddressFilter extends OncePerRequestFilter {

    static final String ATRIBUTO = SocketAddressFilter.class.getName() + ".socketIp";

    /**
     * O endereço real da conexão.
     *
     * <p>Cai para {@code getRemoteAddr()} só quando o atributo não existe, o que
     * acontece fora da cadeia de filtros — num teste unitário com
     * {@code MockHttpServletRequest}, por exemplo. Nesse caminho não há embrulho
     * nenhum, então os dois valores são o mesmo.
     */
    public static String socketIpDe(HttpServletRequest request) {
        Object guardado = request.getAttribute(ATRIBUTO);
        return guardado instanceof String ip ? ip : request.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(ATRIBUTO, request.getRemoteAddr());
        filterChain.doFilter(request, response);
    }
}
