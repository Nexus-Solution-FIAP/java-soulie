package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.ChatReply;
import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.chat.ChatApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class ChatController {

    @Autowired
    private ChatApiService chatApiService;

    @PostMapping("/soulie-ia")
    public ResponseEntity<ChatReply> soulieIa(@AuthenticationPrincipal User user, @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatApiService.reply(user, request));
    }
}
