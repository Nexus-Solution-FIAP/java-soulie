package br.com.fiap.soulie.infra.cloudflare;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Descobre o IP real de quem chamou, quando a API está atrás do Cloudflare.
 *
 * <p><b>Sem isto, o rate limit por IP é inútil:</b> atrás de um proxy, o
 * endereço da conexão é o do <i>proxy</i>, igual para todo mundo — então o
 * limite estrangularia todos os usuários no mesmo balde.
 *
 * <p><b>E aceitar o cabeçalho sem conferir a origem é pior ainda:</b> aí
 * qualquer cliente manda um {@code CF-Connecting-IP} diferente a cada
 * requisição e o limite vira decoração. Por isso o cabeçalho só é lido quando a
 * <b>conexão</b> vem de uma faixa do Cloudflare.
 *
 * <p>Essa verificação usa o endereço guardado pelo {@link SocketAddressFilter},
 * e <b>não</b> {@code getRemoteAddr()}. A diferença é o bypass que existiu aqui:
 * com o {@code ForwardedHeaderFilter} embrulhando a requisição,
 * {@code getRemoteAddr()} passa a devolver o que o cliente escreveu em
 * {@code X-Forwarded-For}. O atributo é gravado antes de qualquer embrulho.
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private static final String HEADER_CLOUDFLARE = "CF-Connecting-IP";

    /**
     * IPv4 em quatro octetos decimais.
     *
     * <p>Usado para separar literal de IP de <b>nome de domínio</b>. Ver
     * {@link #ehLiteralDeIp(String)}.
     */
    private static final Pattern IPV4 = Pattern.compile(
            "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}");

    /**
     * Faixas publicadas pelo Cloudflare (https://www.cloudflare.com/ips/).
     *
     * <p>Embutidas para a aplicação funcionar sem configuração, e
     * sobrescrevíveis por {@code soulie.cloudflare.trusted-ips} porque elas
     * mudam de tempos em tempos.
     */
    private static final List<String> FAIXAS_CLOUDFLARE = List.of(
            "173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
            "141.101.64.0/18", "108.162.192.0/18", "190.93.240.0/20", "188.114.96.0/20",
            "197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
            "104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22",
            "2400:cb00::/32", "2606:4700::/32", "2803:f800::/32", "2405:b500::/32",
            "2405:8100::/32", "2a06:98c0::/29", "2c0f:f248::/32");

    private final CloudflareProperties properties;
    private final List<FaixaCidr> faixasConfiaveis = new ArrayList<>();

    public ClientIpResolver(CloudflareProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void carregarFaixas() {
        List<String> configuradas = properties.getTrustedIps() == null
                ? List.of()
                : properties.getTrustedIps().stream()
                        // Elemento branco não pode chegar ao parse:
                        // InetAddress.getByName("") devolve LOOPBACK, então uma
                        // vírgula sobrando em CLOUDFLARE_TRUSTED_IPS trocaria as
                        // faixas do Cloudflare por 127.0.0.1 — em silêncio, e
                        // com o boot verde.
                        .filter(cidr -> cidr != null && !cidr.isBlank())
                        .toList();

        List<String> origem = configuradas.isEmpty() ? FAIXAS_CLOUDFLARE : configuradas;

        for (String cidr : origem) {
            try {
                faixasConfiaveis.add(FaixaCidr.parse(cidr.trim()));
            } catch (Exception exception) {
                // Faixa malformada é ignorada com aviso, não derruba o boot: o
                // efeito de ignorá-la é deixar de confiar no cabeçalho vindo
                // dali, que é o lado seguro do erro.
                log.warn("Faixa de IP confiável inválida e ignorada: {}", cidr);
            }
        }

        if (properties.isEnabled() && faixasConfiaveis.isEmpty()) {
            // Mesmo raciocínio do validarCors do SecurityConfig: configuração
            // que não faz o que promete recusa a subida, em vez de rodar
            // parecendo protegida. Aqui o efeito seria pior que inútil —
            // ninguém seria confiável, todos cairiam no IP do proxy, e o limite
            // estrangularia a base inteira num balde só.
            throw new IllegalStateException(
                    "soulie.cloudflare.enabled=true mas nenhuma faixa confiável válida foi carregada. "
                            + "Revise CLOUDFLARE_TRUSTED_IPS.");
        }
        if (properties.isEnabled()) {
            log.info("Confiança em {} ativada para {} faixas", HEADER_CLOUDFLARE, faixasConfiaveis.size());
        }
    }

    /**
     * O IP do cliente.
     *
     * @return o valor de {@code CF-Connecting-IP} quando a conexão vem de faixa
     *         confiável; caso contrário o endereço real da conexão
     */
    public String resolver(HttpServletRequest request) {
        String ipDoSocket = SocketAddressFilter.socketIpDe(request);

        if (!properties.isEnabled()) {
            return ipDoSocket;
        }

        String cabecalho = request.getHeader(HEADER_CLOUDFLARE);
        if (cabecalho == null || cabecalho.isBlank()) {
            return ipDoSocket;
        }

        if (!vemDeProxyConfiavel(ipDoSocket)) {
            // Alguém mandou o cabeçalho sem vir pelo proxy. Não é erro de
            // configuração: é tentativa de burlar o limite.
            log.debug("CF-Connecting-IP recebido de origem não confiável, ignorado");
            return ipDoSocket;
        }

        String informado = cabecalho.trim();
        if (!ehLiteralDeIp(informado)) {
            // O cabeçalho vem do proxy, mas o conteúdo dele vem do cliente. Sem
            // esta checagem, qualquer string entraria na chave do contador — e,
            // pior, seria resolvida como nome de domínio mais abaixo.
            log.debug("CF-Connecting-IP com valor que não é endereço IP, ignorado");
            return ipDoSocket;
        }
        return informado;
    }

    /** True se a conexão veio de uma das faixas confiáveis. */
    public boolean vemDeProxyConfiavel(String ip) {
        if (!ehLiteralDeIp(ip)) {
            return false;
        }
        try {
            InetAddress endereco = InetAddress.getByName(ip);
            return faixasConfiaveis.stream().anyMatch(faixa -> faixa.contem(endereco));
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    /**
     * É um endereço IP escrito por extenso, e não um nome?
     *
     * <p>Esta checagem existe porque {@code InetAddress.getByName} <b>resolve
     * nome de domínio</b>, não só literal. Sem ela, o valor que chega num
     * cabeçalho podia virar uma consulta DNS <b>na thread da requisição e sem
     * timeout</b>, com três consequências: quem controla um domínio o aponta
     * para um IP de faixa Cloudflare e passa a ser "confiável"; um rótulo
     * aleatório por requisição ignora o cache e pendura threads até esgotar o
     * pool; e o servidor passa a consultar domínios escolhidos por terceiro.
     *
     * <p>Um nome nunca contém {@code ':'}, então a presença de dois-pontos já
     * separa IPv6 de nome. O resto tem de casar o IPv4 por extenso.
     */
    private static boolean ehLiteralDeIp(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String limpo = valor.trim();
        if (limpo.indexOf(':') >= 0) {
            return true;
        }
        return IPV4.matcher(limpo).matches();
    }

    /** Faixa CIDR, comparada bit a bit — serve para IPv4 e IPv6. */
    record FaixaCidr(byte[] base, int bitsDePrefixo) {

        static FaixaCidr parse(String cidr) throws UnknownHostException {
            String[] partes = cidr.split("/");
            InetAddress endereco = InetAddress.getByName(partes[0]);
            byte[] base = endereco.getAddress();
            int bits = partes.length > 1
                    ? Integer.parseInt(partes[1])
                    : base.length * 8;

            // Prefixo fora da faixa tem de ser recusado AQUI, para cair no catch
            // de quem chama e a faixa ser ignorada. Antes isto passava, porque
            // Integer.parseInt("33") não falha — e o estrago aparecia depois:
            // /33 estourava ArrayIndexOutOfBounds em toda requisição daquele IP,
            // com o boot verde, e /-1 era pior porque NÃO falhava: a máscara
            // degenerava e metade do espaço IPv4 passava a ser "Cloudflare".
            if (bits < 0 || bits > base.length * 8) {
                throw new IllegalArgumentException(
                        "Prefixo CIDR fora da faixa para este endereço: " + cidr);
            }
            return new FaixaCidr(base, bits);
        }

        boolean contem(InetAddress candidato) {
            byte[] bytes = candidato.getAddress();
            // IPv4 nunca cai numa faixa IPv6 e vice-versa.
            if (bytes.length != base.length) {
                return false;
            }
            int bytesInteiros = bitsDePrefixo / 8;
            int bitsSoltos = bitsDePrefixo % 8;

            for (int i = 0; i < bytesInteiros; i++) {
                if (bytes[i] != base[i]) {
                    return false;
                }
            }
            if (bitsSoltos == 0) {
                return true;
            }
            int mascara = 0xFF << (8 - bitsSoltos);
            return (bytes[bytesInteiros] & mascara) == (base[bytesInteiros] & mascara);
        }
    }
}
