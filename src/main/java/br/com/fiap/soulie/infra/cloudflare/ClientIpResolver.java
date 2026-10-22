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

/**
 * Descobre o IP real de quem chamou, quando a API está atrás do Cloudflare.
 *
 * <p><b>Sem isto, o rate limit por IP é inútil:</b> atrás de um proxy,
 * {@code request.getRemoteAddr()} devolve o IP do <i>proxy</i>, igual para todo
 * mundo — então o limite estrangularia todos os usuários no mesmo balde.
 *
 * <p><b>E aceitar o cabeçalho sem conferir a origem é pior ainda:</b> aí
 * qualquer cliente manda um {@code CF-Connecting-IP} diferente a cada
 * requisição e o limite vira decoração. Por isso o cabeçalho só é lido quando a
 * <b>conexão</b> vem de uma faixa do Cloudflare — e essa verificação usa o IP do
 * socket, que o cliente não controla.
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private static final String HEADER_CLOUDFLARE = "CF-Connecting-IP";

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
        List<String> origem = properties.getTrustedIps() == null || properties.getTrustedIps().isEmpty()
                ? FAIXAS_CLOUDFLARE
                : properties.getTrustedIps();

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
        if (properties.isEnabled()) {
            log.info("Confiança em {} ativada para {} faixas", HEADER_CLOUDFLARE, faixasConfiaveis.size());
        }
    }

    /**
     * O IP do cliente.
     *
     * @return o valor de {@code CF-Connecting-IP} quando a conexão vem de faixa
     *         confiável; caso contrário o IP do socket, que não é falsificável
     */
    public String resolver(HttpServletRequest request) {
        String ipDoSocket = request.getRemoteAddr();

        if (!properties.isEnabled()) {
            return ipDoSocket;
        }

        String cabecalho = request.getHeader(HEADER_CLOUDFLARE);
        if (cabecalho == null || cabecalho.isBlank()) {
            return ipDoSocket;
        }

        if (!vemDoCloudflare(ipDoSocket)) {
            // Alguém mandou o cabeçalho sem vir pelo proxy. Não é erro de
            // configuração: é tentativa de burlar o limite.
            log.debug("CF-Connecting-IP recebido de origem não confiável ({}), ignorado", ipDoSocket);
            return ipDoSocket;
        }

        return cabecalho.trim();
    }

    /** True se a conexão veio de uma das faixas confiáveis. */
    public boolean vemDoCloudflare(String ip) {
        try {
            InetAddress endereco = InetAddress.getByName(ip);
            return faixasConfiaveis.stream().anyMatch(faixa -> faixa.contem(endereco));
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    /** Faixa CIDR, comparada bit a bit — serve para IPv4 e IPv6. */
    record FaixaCidr(byte[] base, int bitsDePrefixo) {

        static FaixaCidr parse(String cidr) throws UnknownHostException {
            String[] partes = cidr.split("/");
            InetAddress endereco = InetAddress.getByName(partes[0]);
            int bits = partes.length > 1
                    ? Integer.parseInt(partes[1])
                    : endereco.getAddress().length * 8;
            return new FaixaCidr(endereco.getAddress(), bits);
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
