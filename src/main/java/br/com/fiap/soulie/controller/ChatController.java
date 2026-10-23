package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.ChatReply;
import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.chat.ChatApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/chat", "/v1/chat"})
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Soulie IA", description = "Alma: LLM ancorado nas missões e no ranking reais; sem chave, cai no FAQ")
public class ChatController {

    @Autowired
    private ChatApiService chatApiService;

    @PostMapping("/soulie-ia")
    @Operation(
            summary = "Pergunta para a Soulie IA",
            description = "Envie a mensagem e, se a tela tiver uma missão aberta, o contexto dela. Chips de missões, ranking e XP respondem com dado do banco. Texto livre passa pelo LLM quando configurado.")
    public ResponseEntity<ChatReply> soulieIa(@Parameter(hidden = true) @AuthenticationPrincipal User user, @RequestBody @Valid ChatRequest request) {
        return ResponseEntity.ok(chatApiService.reply(user, request));
    }
}
