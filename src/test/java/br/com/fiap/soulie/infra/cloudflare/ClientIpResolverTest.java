package br.com.fiap.soulie.infra.cloudflare;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A lógica de confiança do resolvedor, isolada.
 *
 * <p>Atrás de um proxy, o endereço da conexão é o do proxy — igual para todo
 * mundo —, então sem ler {@code CF-Connecting-IP} o limite por IP estrangularia
 * todos os usuários juntos. Mas aceitar esse cabeçalho de qualquer origem é
 * pior: aí qualquer cliente manda um IP diferente por requisição e nunca bate no
 * limite. A regra é ler o cabeçalho <b>só quando a conexão vem de faixa do
 * Cloudflare</b>.
 *
 * <p><b>O que esta classe NÃO prova.</b> Ela monta a requisição com
 * {@code setRemoteAddr}, que por construção não é falsificável — ou seja,
 * assume como verdade exatamente o que o {@code ForwardedHeaderFilter} quebrava.
 * A versão anterior deste arquivo codificava essa premissa e por isso era
 * estruturalmente incapaz de detectar o bypass real. O que fecha esse buraco é o
 * {@code RateLimitBypassTest}, que atravessa a cadeia de filtros de verdade.
 * Aqui ficam as bordas de parsing e a decisão de confiança em si.
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

    private ClientIpResolver comFaixas(String... cidrs) {
        var proprias = new CloudflareProperties();
        proprias.setEnabled(true);
        proprias.setTrustedIps(Arrays.asList(cidrs));
        var proprio = new ClientIpResolver(proprias);
        proprio.carregarFaixas();
        return proprio;
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
    @DisplayName("Cabecalho vindo de fora da faixa Cloudflare e ignorado")
    void cabecalhoDeForaEhIgnorado() {
        var request = requisicao("203.0.113.77", "1.2.3.4");

        assertThat(resolver.resolver(request))
                .as("o endereço da conexão é o que vale quando a origem não é confiável")
                .isEqualTo("203.0.113.77");
    }

    @Test
    @DisplayName("Cabecalho vindo de faixa Cloudflare e aceito")
    void cabecalhoDoCloudflareEhAceito() {
        // 162.158.0.0/15 é uma das faixas publicadas pelo Cloudflare.
        var request = requisicao("162.158.1.1", "198.51.100.9");

        assertThat(resolver.resolver(request)).isEqualTo("198.51.100.9");
    }

    @Test
    @DisplayName("Com a confianca desligada, o cabecalho nunca e lido")
    void desligadoIgnoraCabecalhoAteDeOrigemConfiavel() {
        properties.setEnabled(false);

        var request = requisicao("162.158.1.1", "198.51.100.9");

        assertThat(resolver.resolver(request)).isEqualTo("162.158.1.1");
    }

    @Test
    @DisplayName("Sem o cabecalho, usa o endereco da conexao")
    void semCabecalhoUsaSocket() {
        assertThat(resolver.resolver(requisicao("162.158.1.1", null))).isEqualTo("162.158.1.1");
    }

    @Test
    @DisplayName("IPv4 nao casa com faixa IPv6 e vice-versa")
    void naoConfundeFamiliasDeEndereco() {
        // 2400:cb00::/32 é faixa IPv6 do Cloudflare.
        assertThat(resolver.vemDeProxyConfiavel("2400:cb00::1")).isTrue();
        assertThat(resolver.vemDeProxyConfiavel("1.2.3.4")).isFalse();
        assertThat(resolver.vemDeProxyConfiavel("162.158.1.1")).isTrue();
        // Faixa IPv4 não pode casar endereço IPv6 e vice-versa, mesmo com /0.
        assertThat(comFaixas("0.0.0.0/0").vemDeProxyConfiavel("2400:cb00::1")).isFalse();
    }

    @Test
    @DisplayName("Faixas customizadas substituem as embutidas")
    void faixasCustomizadasSubstituem() {
        var proprio = comFaixas("10.0.0.0/8");

        assertThat(proprio.resolver(requisicao("10.1.2.3", "8.8.8.8"))).isEqualTo("8.8.8.8");
        // A faixa do Cloudflare deixa de ser confiável quando há lista própria.
        assertThat(proprio.resolver(requisicao("162.158.1.1", "8.8.8.8"))).isEqualTo("162.158.1.1");
    }

    @Test
    @DisplayName("Faixa malformada e ignorada e nao derruba o boot")
    void faixaMalformadaNaoDerruba() {
        var proprio = comFaixas("isso-nao-e-cidr", "10.0.0.0/8");

        // A boa continua valendo; ignorar a ruim é o lado seguro do erro.
        assertThat(proprio.vemDeProxyConfiavel("10.1.2.3")).isTrue();
    }

    @Test
    @DisplayName("BYPASS: nome de dominio nao e resolvido para decidir confianca")
    void nomeDeDominioNaoEhResolvido() {
        // InetAddress.getByName RESOLVE NOME, não só literal. Sem a checagem de
        // formato, "localhost" virava 127.0.0.1, caía numa faixa de loopback
        // configurada e o cabeçalho forjado passava a valer. Pior: com um nome
        // controlado pelo atacante apontando para IP de faixa Cloudflare, a
        // confiança era concedida — e cada requisição gerava uma consulta DNS,
        // sem timeout, na thread do servidor.
        var proprio = comFaixas("127.0.0.0/8");

        assertThat(proprio.vemDeProxyConfiavel("localhost")).isFalse();
        assertThat(proprio.vemDeProxyConfiavel("evil.example.com")).isFalse();
        // "beef.cafe" só tem caracteres hexadecimais: uma checagem por lista de
        // caracteres permitidos passaria batido aqui.
        assertThat(proprio.vemDeProxyConfiavel("beef.cafe")).isFalse();
        assertThat(proprio.vemDeProxyConfiavel("127.0.0.1")).isTrue();
    }

    @Test
    @DisplayName("BYPASS: valor nao-IP no cabecalho e recusado")
    void valorNaoIpNoCabecalhoEhRecusado() {
        var proprio = comFaixas("10.0.0.0/8");

        // O cabeçalho vem do proxy, mas o CONTEÚDO dele vem do cliente. Sem
        // esta checagem qualquer string entrava na chave do contador.
        assertThat(proprio.resolver(requisicao("10.1.2.3", "nao-sou-um-ip")))
                .isEqualTo("10.1.2.3");
        assertThat(proprio.resolver(requisicao("10.1.2.3", "a".repeat(900))))
                .as("chave do contador não pode ter tamanho definido pelo cliente")
                .isEqualTo("10.1.2.3");
    }

    @Test
    @DisplayName("Prefixo CIDR fora da faixa e recusado, nao estoura depois")
    void prefixoForaDaFaixaEhRecusado() {
        // /33 num IPv4 passava no Integer.parseInt, subia o boot, e depois
        // estourava ArrayIndexOutOfBoundsException em TODA requisição cujo IP
        // casasse os bytes da base — 500 permanente com o boot verde.
        var trintaETres = comFaixas("10.0.0.0/33", "192.168.0.0/16");
        assertThat(trintaETres.vemDeProxyConfiavel("10.0.0.0")).isFalse();
        assertThat(trintaETres.vemDeProxyConfiavel("192.168.1.1"))
                .as("a faixa válida da mesma lista continua valendo")
                .isTrue();

        assertThat(comFaixas("2400:cb00::/129", "10.0.0.0/8").vemDeProxyConfiavel("2400:cb00::1"))
                .isFalse();
    }

    @Test
    @DisplayName("BYPASS: prefixo negativo nao alarga a faixa confiavel")
    void prefixoNegativoNaoAlargaAFaixa() {
        // Este era o pior dos dois, porque NÃO falhava: com /-1 a máscara
        // degenerava para "compare só o bit de sinal", e toda a metade
        // 0.0.0.0-127.255.255.255 passava a ser "Cloudflare". Um typo em
        // CLOUDFLARE_TRUSTED_IPS abria o bypass em silêncio.
        var negativo = comFaixas("10.0.0.0/-1", "192.168.0.0/16");

        assertThat(negativo.vemDeProxyConfiavel("9.9.9.9")).isFalse();
        assertThat(negativo.vemDeProxyConfiavel("127.0.0.1")).isFalse();
        assertThat(negativo.vemDeProxyConfiavel("192.168.1.1")).isTrue();
    }

    @Test
    @DisplayName("Elemento branco na lista nao vira loopback confiavel")
    void elementoBrancoNaoViraLoopback() {
        // InetAddress.getByName("") devolve LOOPBACK. Como a lista só caía no
        // fallback embutido quando estava VAZIA, uma vírgula sobrando em
        // CLOUDFLARE_TRUSTED_IPS produzia lista não-vazia com elemento branco e
        // trocava as faixas do Cloudflare por 127.0.0.1 — sem erro, com o boot
        // logando "ativada para 1 faixas".
        var comBranco = comFaixas("", "  ");

        assertThat(comBranco.vemDeProxyConfiavel("127.0.0.1")).isFalse();
        assertThat(comBranco.vemDeProxyConfiavel("162.158.1.1"))
                .as("lista efetivamente vazia volta para as faixas embutidas")
                .isTrue();
    }

    @Test
    @DisplayName("Confianca ligada sem nenhuma faixa valida recusa o boot")
    void semFaixaValidaRecusaOBoot() {
        var soLixo = new CloudflareProperties();
        soLixo.setEnabled(true);
        soLixo.setTrustedIps(List.of("isso-nao-e-cidr", "10.0.0.0/99"));
        var quebrado = new ClientIpResolver(soLixo);

        // Subir assim seria pior que inútil: ninguém confiável, todos caindo no
        // IP do proxy, e o limite estrangulando a base inteira num balde só.
        assertThatThrownBy(quebrado::carregarFaixas)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLOUDFLARE_TRUSTED_IPS");
    }

    @Test
    @DisplayName("Bordas de prefixo bem-formado continuam corretas")
    void bordasDePrefixoBemFormado() {
        assertThat(comFaixas("10.0.0.0/0").vemDeProxyConfiavel("8.8.8.8")).isTrue();
        assertThat(comFaixas("10.1.2.3/32").vemDeProxyConfiavel("10.1.2.3")).isTrue();
        assertThat(comFaixas("10.1.2.3/32").vemDeProxyConfiavel("10.1.2.4")).isFalse();
        // Prefixo não múltiplo de 8, nas duas bordas exatas.
        assertThat(comFaixas("2a06:98c0::/29").vemDeProxyConfiavel("2a06:98c7:ffff::1")).isTrue();
        assertThat(comFaixas("2a06:98c0::/29").vemDeProxyConfiavel("2a06:98c8::1")).isFalse();
        assertThat(comFaixas("2400:cb00::1/128").vemDeProxyConfiavel("2400:cb00::1")).isTrue();
        // IPv4 mapeado em IPv6 é normalizado para 4 bytes pelo Java.
        assertThat(comFaixas("162.158.0.0/15").vemDeProxyConfiavel("::ffff:162.158.1.1")).isTrue();
    }
}
