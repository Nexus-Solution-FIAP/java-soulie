package br.com.fiap.soulie.validations.chat;

import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageLengthValidator implements ChatRequestValidator {

    static final int MAXIMO = 500;

    @Override
    public void validate(User user, ChatRequest request) {
        if (request != null && request.message() != null && request.message().length() > MAXIMO) {
            throw new ValidationException("Mensagem muito longa");
        }
    }
}
