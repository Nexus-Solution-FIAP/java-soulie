package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.domain.user.dto.UserResponse;
import br.com.fiap.soulie.infra.security.RestAuthenticationEntryPoint;
import br.com.fiap.soulie.infra.security.SecurityConfig;
import br.com.fiap.soulie.infra.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

// Configuração comum dos testes de controller:
// - @Import traz as regras de segurança reais do projeto (rotas liberadas/protegidas)
// - TokenService e UserRepository são dependências do SecurityFilter, então viram mocks
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})
@ActiveProfiles("test")
abstract class ControllerTestBase {

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected TokenService tokenService;

    @MockitoBean
    protected UserRepository userRepository;

    protected final User loggedUser = TestData.user(1L);

    // Simula um usuário autenticado; é ele que chega no @AuthenticationPrincipal
    protected RequestPostProcessor logged() {
        return user(loggedUser);
    }

    protected UserResponse userResponse(Long id) {
        return new UserResponse(id, "Usuário " + id, 0, true);
    }
}
