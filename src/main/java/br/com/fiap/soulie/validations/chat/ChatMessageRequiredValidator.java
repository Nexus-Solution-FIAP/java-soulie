package br.com.fiap.soulie.validations.chat;

import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ChatMessageRequiredValidator implements ChatRequestValidator {

    @Override
    public void validate(User user, ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ValidationException("Mensagem é obrigatória");
        }
    }
}
