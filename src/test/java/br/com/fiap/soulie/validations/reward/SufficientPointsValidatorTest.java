package br.com.fiap.soulie.validations.reward;

import br.com.fiap.soulie.TestData;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SufficientPointsValidatorTest {

    private final SufficientPointsValidator validator = new SufficientPointsValidator();

    @Test
    void passesWhenBalanceIsHigher() {
        assertThatCode(() -> validator.validate(
                TestData.rewardCatalog(1L, "Brinde", 50), TestData.userWithPoints(1L, 100)))
                .doesNotThrowAnyException();
    }

    @Test
    void passesWhenBalanceIsExactlyTheCost() {
        assertThatCode(() -> validator.validate(
                TestData.rewardCatalog(1L, "Brinde", 100), TestData.userWithPoints(1L, 100)))
                .doesNotThrowAnyException();
    }

    @Test
    void failsWhenBalanceIsLower() {
        assertThatThrownBy(() -> validator.validate(
                TestData.rewardCatalog(1L, "Brinde", 101), TestData.userWithPoints(1L, 100)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Pontos insuficientes para resgatar esta recompensa");
    }
}
