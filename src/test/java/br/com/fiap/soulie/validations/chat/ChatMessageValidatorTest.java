package br.com.fiap.soulie.validations.chat;

import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageValidatorTest {

    private final User user = User.builder().id(1L).name("Ana").build();
    private final ChatMessageRequiredValidator required = new ChatMessageRequiredValidator();
    private final ChatMessageLengthValidator length = new ChatMessageLengthValidator();

    @Test
    void mensagemPresentePassa() {
        var request = new ChatRequest("Oi", null);
        assertThatCode(() -> required.validate(user, request)).doesNotThrowAnyException();
        assertThatCode(() -> length.validate(user, request)).doesNotThrowAnyException();
    }

    @Test
    void mensagemVaziaFalha() {
        assertThatThrownBy(() -> required.validate(user, new ChatRequest("  ", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obrigatória");
    }

    @Test
    void mensagemLongaFalha() {
        String enorme = "a".repeat(ChatMessageLengthValidator.MAXIMO + 1);
        assertThatThrownBy(() -> length.validate(user, new ChatRequest(enorme, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("longa");
    }
}
