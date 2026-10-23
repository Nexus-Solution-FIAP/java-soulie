package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.ChatReply;
import br.com.fiap.soulie.domain.chat.ChatApiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest extends ControllerTestBase {

    @MockitoBean
    private ChatApiService chatApiService;

    @Test
    @DisplayName("Pergunta autenticada devolve reply")
    void perguntaComTokenDevolveReply() throws Exception {
        when(chatApiService.reply(eq(loggedUser), any())).thenReturn(new ChatReply("Olá, guardião."));

        mvc.perform(post("/v1/chat/soulie-ia").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Quais missões tenho?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Olá, guardião."));
    }

    @Test
    @DisplayName("Sem token a Alma não responde")
    void semTokenDevolve401() throws Exception {
        mvc.perform(post("/v1/chat/soulie-ia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Oi"}
                                """))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(chatApiService);
    }

    @Test
    @DisplayName("Mensagem vazia é 400")
    void mensagemVaziaDevolve400() throws Exception {
        mvc.perform(post("/v1/chat/soulie-ia").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "   "}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(chatApiService);
    }

    @Test
    @DisplayName("O mesmo contrato responde em /api/v1")
    void prefixoDuploFunciona() throws Exception {
        when(chatApiService.reply(eq(loggedUser), any())).thenReturn(new ChatReply("ok"));

        mvc.perform(post("/api/v1/chat/soulie-ia").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Oi"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("ok"));
    }
}
