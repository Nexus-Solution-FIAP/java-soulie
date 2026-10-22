package br.com.fiap.soulie.validations.reward;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SufficientPointsValidatorTest {

    private final SufficientPointsValidator validator = new SufficientPointsValidator();

    @Test
    void passesWhenBalanceIsHigher() {
        assertThatCode(() -> validator.validate(new RewardRequest("Brinde", 50), TestData.userWithPoints(1L, 100)))
                .doesNotThrowAnyException();
    }

    @Test
    void passesWhenBalanceIsExactlyTheCost() {
        assertThatCode(() -> validator.validate(new RewardRequest("Brinde", 100), TestData.userWithPoints(1L, 100)))
                .doesNotThrowAnyException();
    }

    @Test
    void failsWhenBalanceIsLower() {
        assertThatThrownBy(() -> validator.validate(new RewardRequest("Brinde", 101), TestData.userWithPoints(1L, 100)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Pontos insuficientes para resgatar esta recompensa");
    }
}
