package br.com.fiap.soulie.infra.ratelimit;

import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A chave do contador numa rota autenticada é a identidade, não o IP.
 *
 * <p>Isto era <b>falso</b> na primeira versão da Fase 6, embora o javadoc do
 * filtro afirmasse o contrário. O {@code RateLimitFilter} roda antes do
 * {@code SecurityFilter} de propósito — para o login já chegar protegido —, mas
 * é o {@code SecurityFilter} que popula o {@code SecurityContextHolder}, e a API
 * é {@code STATELESS}. Logo o contexto estava <b>sempre vazio</b> ali e o ramo
 * que lia o principal era código morto: toda regra acabava chaveada por IP.
 *
 * <p>Isso quebrava as duas pontas. Quem rotacionava IP ignorava os limites de
 * custo — os 10/hora de validação por IA, que é chamada paga — e, no sentido
 * inverso, todos atrás de um mesmo NAT dividiam o mesmo balde, que é exatamente
 * o que o comentário prometia não acontecer.
 *
 * <p><b>Por que o token é real aqui.</b> O post-processor {@code user(...)} do
 * {@code spring-security-test} injeta o contexto <i>antes</i> da cadeia de
 * filtros, então um teste escrito com ele <b>passaria mentindo</b>: provaria uma
 * leitura de contexto que em produção nunca acontece. Este teste usa o
 * {@code TokenService} de verdade e um token assinado de verdade.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "soulie.ratelimit.enabled=true",
        "soulie.ratelimit.distributed=false",
        "soulie.ratelimit.rules[0].method=GET",
        "soulie.ratelimit.rules[0].path=/v1/social/followers",
        "soulie.ratelimit.rules[0].capacity=2",
        "soulie.ratelimit.rules[0].window=PT1M"
        // sem by-ip: esta regra DEVE ser por identidade
})
class RateLimitPorUsuarioTest {

    @Autowired private MockMvc mvc;
    @Autowired private TokenService tokenService;

    @MockitoBean private UserRepository userRepository;

    private String token;

    @BeforeEach
    void setUp() {
        var usuario = User.builder()
                .id(1L)
                .name("Ana")
                .email("ana@soulie.com")
                .password("irrelevante")
                .active(true)
                .build();

        token = tokenService.generateToken(usuario);
        when(userRepository.findByEmail(anyString())).thenReturn(usuario);
    }

    @Test
    @DisplayName("Mesmo usuario em IPs diferentes compartilha o balde")
    void mesmoUsuarioEmIpsDiferentesCompartilhaOBalde() throws Exception {
        // O ataque que isto fecha: rotacionar IP para ganhar balde novo. Antes
        // da correção estas 4 chamadas devolviam 200 com remaining=1 em TODAS.
        for (int i = 1; i <= 2; i++) {
            mvc.perform(get("/v1/social/followers")
                    .with(r -> { r.setRemoteAddr("198.51.100.200"); return r; })
                    .header("Authorization", "Bearer " + token));
        }

        mvc.perform(get("/v1/social/followers")
                        .with(r -> { r.setRemoteAddr("198.51.100.201"); return r; })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Usuarios diferentes no mesmo IP tem baldes separados")
    void usuariosDiferentesNoMesmoIpTemBaldesSeparados() throws Exception {
        // O outro lado: quem compartilha saída de rede não pode ser punido pelo
        // consumo do vizinho.
        var outro = User.builder()
                .id(2L).name("Bruno").email("bruno@soulie.com")
                .password("irrelevante").active(true).build();
        String tokenDoOutro = tokenService.generateToken(outro);

        for (int i = 1; i <= 3; i++) {
            mvc.perform(get("/v1/social/followers")
                    .with(r -> { r.setRemoteAddr("198.51.100.210"); return r; })
                    .header("Authorization", "Bearer " + token));
        }

        when(userRepository.findByEmail("bruno@soulie.com")).thenReturn(outro);
        mvc.perform(get("/v1/social/followers")
                        .with(r -> { r.setRemoteAddr("198.51.100.210"); return r; })
                        .header("Authorization", "Bearer " + tokenDoOutro))
                .andExpect(naoFoiBarrado());
    }

    /** Qualquer status menos 429 — o que importa é não ter sido barrado. */
    private static org.springframework.test.web.servlet.ResultMatcher naoFoiBarrado() {
        return resultado -> {
            if (resultado.getResponse().getStatus() == 429) {
                throw new AssertionError("o balde de um usuário vazou para o outro");
            }
        };
    }

    @Test
    @DisplayName("Token invalido cai para o limite por IP, nao ganha balde proprio")
    void tokenInvalidoCaiParaOLimitePorIp() throws Exception {
        // Sem identidade comprovada, o único identificador honesto é o IP. Um
        // token forjado diferente por requisição não pode virar balde novo.
        for (int i = 1; i <= 2; i++) {
            mvc.perform(get("/v1/social/followers")
                    .with(r -> { r.setRemoteAddr("198.51.100.220"); return r; })
                    .header("Authorization", "Bearer token.invalido." + i));
        }

        mvc.perform(get("/v1/social/followers")
                        .with(r -> { r.setRemoteAddr("198.51.100.220"); return r; })
                        .header("Authorization", "Bearer token.invalido.outro"))
                .andExpect(status().isTooManyRequests());
    }
}
