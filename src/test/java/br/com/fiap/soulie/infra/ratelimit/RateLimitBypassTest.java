package br.com.fiap.soulie.infra.ratelimit;

import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os bypasses do rate limit, cada um escrito para falhar antes da correção.
 *
 * <p>Esta classe existe porque a Fase 6 foi entregue com o limite
 * <b>inteiramente contornável</b> e a suíte verde. O
 * {@code RateLimitIntegrationTest} testava variação de {@code CF-Connecting-IP},
 * que é o cabeçalho óbvio, e não tocava em {@code X-Forwarded-For} — que era o
 * que funcionava, porque o {@code ForwardedHeaderFilter} sobrescrevia
 * {@code getRemoteAddr()} com ele.
 *
 * <p>Todos os cenários aqui atravessam a <b>cadeia real de filtros</b>. Nenhum
 * usa {@code setRemoteAddr} como se ele fosse inforjável, que foi a premissa
 * falsa sobre a qual o teste original foi construído.
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
class RateLimitBypassTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private TokenService tokenService;

    private static final String CREDENCIAIS = """
            {"login":"alguem@soulie.com","password":"SenhaErrada@1"}
            """;


    /**
     * CUIDADO AO ADICIONAR TESTE AQUI: use IP que nenhuma outra classe use.
     *
     * <p>Esta classe tem as mesmas propriedades do
     * {@code RateLimitIntegrationTest}, entao o Spring reaproveita o MESMO
     * contexto para as duas — e com ele o mesmo {@code InMemoryRateLimitStore}.
     * Os contadores sobrevivem entre as classes. Repetir um IP faz um teste
     * encontrar o balde ja gasto pelo outro, e a ordem de execucao decide quem
     * falha. Por isso esta classe fica na faixa 203.0.113.10x.
     */
    private MockHttpServletRequestBuilder login(String socket) {
        return post("/login")
                .with(r -> { r.setRemoteAddr(socket); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREDENCIAIS);
    }

    @Test
    @DisplayName("BYPASS: X-Forwarded-For variando nao ganha balde novo")
    void xForwardedForNaoGanhaBaldeNovo() throws Exception {
        // O ataque: um unico host manda um IP diferente por requisicao. Antes da
        // correcao isto dava 401 indefinidamente, com X-RateLimit-Remaining: 2
        // em TODAS as chamadas -- balde novo a cada vez, forca bruta sem teto.
        for (int i = 1; i <= 3; i++) {
            mvc.perform(login("203.0.113.101").header("X-Forwarded-For", "198.51.100." + i));
        }

        mvc.perform(login("203.0.113.101").header("X-Forwarded-For", "198.51.100.99"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("BYPASS: cabecalho Forwarded padrao tambem nao ganha balde novo")
    void forwardedPadraoNaoGanhaBaldeNovo() throws Exception {
        // O ForwardedHeaderFilter le os DOIS: X-Forwarded-For e o Forwarded do
        // RFC 7239. Fechar so um deixaria o outro aberto.
        for (int i = 1; i <= 3; i++) {
            mvc.perform(login("203.0.113.102").header("Forwarded", "for=198.51.100." + i));
        }

        mvc.perform(login("203.0.113.102").header("Forwarded", "for=198.51.100.99"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("BYPASS: X-Forwarded-For nao pode fingir vir do Cloudflare")
    void xForwardedForNaoFingeOrigemCloudflare() throws Exception {
        // A combinacao que derrotava a logica de confianca: o X-Forwarded-For
        // dizia um IP de faixa publicada do Cloudflare (162.158.0.0/15), o
        // resolvedor acreditava, e com isso o CF-Connecting-IP forjado passava a
        // ser a chave do contador.
        //
        // A confianca esta desligada no perfil de teste, mas o cenario importa
        // de qualquer forma: prova que o IP que decide a confianca nao vem mais
        // de cabecalho nenhum.
        for (int i = 1; i <= 3; i++) {
            mvc.perform(login("203.0.113.103")
                    .header("X-Forwarded-For", "162.158.1.1")
                    .header("CF-Connecting-IP", "10.0.0." + i));
        }

        mvc.perform(login("203.0.113.103")
                        .header("X-Forwarded-For", "162.158.1.1")
                        .header("CF-Connecting-IP", "10.0.0.99"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Controle: sem cabecalho nenhum, o limite funciona")
    void controleSemCabecalho() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mvc.perform(login("203.0.113.104"));
        }
        mvc.perform(login("203.0.113.104")).andExpect(status().isTooManyRequests());
    }
}
