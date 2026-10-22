package br.com.fiap.soulie.infra.ratelimit;

import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O rate limit pelo caminho real: requisição HTTP atravessando o filtro.
 *
 * <p>O perfil de teste desliga o rate limit por padrão — senão cada classe de
 * teste bateria no limite. Aqui ele é ligado explicitamente, com uma faixa
 * apertada, para o comportamento ser observável em poucas requisições.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "soulie.ratelimit.enabled=true",
        "soulie.ratelimit.distributed=false",
        "soulie.ratelimit.rules[0].method=POST",
        "soulie.ratelimit.rules[0].path=/login",
        "soulie.ratelimit.rules[0].capacity=3",
        "soulie.ratelimit.rules[0].window=PT1M",
        "soulie.ratelimit.rules[0].by-ip=true"
})
class RateLimitIntegrationTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private TokenService tokenService;

    private static final String CREDENCIAIS = """
            {"login":"alguem@soulie.com","password":"SenhaErrada@1"}
            """;

    @Test
    @DisplayName("Estourar o limite responde 429 com Retry-After")
    void estourarOLimiteResponde429() throws Exception {
        // As 3 primeiras passam pelo filtro (e falham na autenticação, o que
        // não importa aqui — o que importa é que NÃO são 429).
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/login")
                            .with(r -> { r.setRemoteAddr("203.0.113.10"); return r; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREDENCIAIS))
                    .andExpect(not429());
        }

        // A quarta é barrada pelo rate limit, antes mesmo da autenticação.
        mvc.perform(post("/login")
                        .with(r -> { r.setRemoteAddr("203.0.113.10"); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENCIAIS))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("IPs diferentes têm contadores independentes")
    void contadoresSaoPorIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/login")
                    .with(r -> { r.setRemoteAddr("203.0.113.20"); return r; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CREDENCIAIS));
        }

        // Outro IP começa do zero: o limite não pode ser global.
        mvc.perform(post("/login")
                        .with(r -> { r.setRemoteAddr("203.0.113.21"); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENCIAIS))
                .andExpect(not429());
    }

    @Test
    @DisplayName("Com a confianca desligada, CF-Connecting-IP nao zera o contador")
    void cabecalhoIgnoradoQuandoConfiancaDesligada() throws Exception {
        // Este teste chamava-se "BYPASS: cabeçalho forjado não zera o contador"
        // e NÃO provava isso. A confiança no Cloudflare está desligada no perfil
        // de teste, então ele cobre apenas o caso trivial: o cabeçalho nem é
        // lido. Enquanto ele passava, o rate limit era de fato contornável — por
        // X-Forwarded-For, que nenhum teste tocava.
        //
        // Os bypasses de verdade estão no RateLimitBypassTest. Este fica como
        // guarda do caminho desligado, com o nome dizendo o que ele faz.
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/login")
                    .with(r -> { r.setRemoteAddr("203.0.113.30"); return r; })
                    .header("CF-Connecting-IP", "10.0.0." + i)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CREDENCIAIS));
        }

        mvc.perform(post("/login")
                        .with(r -> { r.setRemoteAddr("203.0.113.30"); return r; })
                        .header("CF-Connecting-IP", "10.0.0.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREDENCIAIS))
                .andExpect(status().isTooManyRequests());
    }

    /** Qualquer status menos 429 — o que importa é não ter sido barrado. */
    private static org.springframework.test.web.servlet.ResultMatcher not429() {
        return resultado -> {
            int status = resultado.getResponse().getStatus();
            if (status == 429) {
                throw new AssertionError("não deveria ter sido barrado pelo rate limit");
            }
        };
    }
}
