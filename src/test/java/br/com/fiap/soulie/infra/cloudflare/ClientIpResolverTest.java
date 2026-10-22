package br.com.fiap.soulie.infra.cloudflare;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste que decide se o rate limit é real ou decoração.
 *
 * <p>Atrás de um proxy, {@code getRemoteAddr()} devolve o IP do proxy — igual
 * para todo mundo —, então sem ler {@code CF-Connecting-IP} o limite por IP
 * estrangularia todos os usuários juntos. Mas aceitar esse cabeçalho de
 * qualquer origem é pior: aí qualquer cliente manda um IP diferente por
 * requisição e nunca bate no limite.
 *
 * <p>A regra é ler o cabeçalho <b>só quando a conexão vem de faixa do
 * Cloudflare</b>, usando o IP do socket — que o cliente não controla.
 */
class ClientIpResolverTest {

    private CloudflareProperties properties;
    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new CloudflareProperties();
        properties.setEnabled(true);
        resolver = new ClientIpResolver(properties);
        resolver.carregarFaixas();
    }

    private HttpServletRequest requisicao(String ipDeOrigem, String cabecalho) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(ipDeOrigem);
        if (cabecalho != null) {
            request.addHeader("CF-Connecting-IP", cabecalho);
        }
        return request;
    }

    @Test
    @DisplayName("BYPASS: cabeçalho forjado de fora da faixa Cloudflare é ignorado")
    void cabecalhoForjadoDeForaEhIgnorado() {
        // É este o ataque: mandar um CF-Connecting-IP diferente a cada
        // requisição para nunca bater no limite. A conexão vem de um IP
        // qualquer da internet, não do Cloudflare.
        var request = requisicao("203.0.113.77", "1.2.3.4");

        assertThat(resolver.resolver(request))
                .as("o IP do socket é o que vale quando a origem não é confiável")
                .isEqualTo("203.0.113.77");
    }

    @Test
    @DisplayName("Cabeçalho vindo de faixa Cloudflare é aceito")
    void cabecalhoDoCloudflareEhAceito() {
        // 162.158.0.0/15 é uma das faixas publicadas pelo Cloudflare.
        var request = requisicao("162.158.1.1", "198.51.100.9");

        assertThat(resolver.resolver(request)).isEqualTo("198.51.100.9");
    }

    @Test
    @DisplayName("Com a confiança desligada, o cabeçalho nunca é lido")
    void desligadoIgnoraCabecalhoAteDeOrigemConfiavel() {
        properties.setEnabled(false);

        var request = requisicao("162.158.1.1", "198.51.100.9");

        assertThat(resolver.resolver(request)).isEqualTo("162.158.1.1");
    }

    @Test
    @DisplayName("Sem o cabeçalho, usa o IP do socket")
    void semCabecalhoUsaSocket() {
        assertThat(resolver.resolver(requisicao("162.158.1.1", null))).isEqualTo("162.158.1.1");
    }

    @Test
    @DisplayName("IPv4 não casa com faixa IPv6 e vice-versa")
    void naoConfundeFamiliasDeEndereco() {
        // 2400:cb00::/32 é faixa IPv6 do Cloudflare.
        assertThat(resolver.vemDoCloudflare("2400:cb00::1")).isTrue();
        assertThat(resolver.vemDoCloudflare("1.2.3.4")).isFalse();
        assertThat(resolver.vemDoCloudflare("162.158.1.1")).isTrue();
    }

    @Test
    @DisplayName("Faixas customizadas substituem as embutidas")
    void faixasCustomizadasSubstituem() {
        var custom = new CloudflareProperties();
        custom.setEnabled(true);
        custom.setTrustedIps(List.of("10.0.0.0/8"));
        var proprio = new ClientIpResolver(custom);
        proprio.carregarFaixas();

        assertThat(proprio.resolver(requisicao("10.1.2.3", "8.8.8.8"))).isEqualTo("8.8.8.8");
        // A faixa do Cloudflare deixa de ser confiável quando há lista própria.
        assertThat(proprio.resolver(requisicao("162.158.1.1", "8.8.8.8"))).isEqualTo("162.158.1.1");
    }

    @Test
    @DisplayName("Faixa malformada é ignorada e não derruba o boot")
    void faixaMalformadaNaoDerruba() {
        var custom = new CloudflareProperties();
        custom.setEnabled(true);
        custom.setTrustedIps(List.of("isso-nao-e-cidr", "10.0.0.0/8"));
        var proprio = new ClientIpResolver(custom);

        proprio.carregarFaixas();

        // A boa continua valendo; ignorar a ruim é o lado seguro do erro.
        assertThat(proprio.vemDoCloudflare("10.1.2.3")).isTrue();
    }
}
