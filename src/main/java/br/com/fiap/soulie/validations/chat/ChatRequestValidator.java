package br.com.fiap.soulie.validations.chat;

import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.user.User;

public interface ChatRequestValidator {

    void validate(User user, ChatRequest request);
}
