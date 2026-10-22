package br.com.fiap.soulie.domain.reward;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.validations.reward.RewardRedeemValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock private RewardRepository rewardRepository;
    @Mock private UserRepository userRepository;
    @Mock private RewardRedeemValidator validator;

    @InjectMocks
    private RewardService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "redeemValidators", List.of(validator));
    }

    @Test
    void redeemSpendsPointsAndSaves() {
        var user = TestData.userWithPoints(1L, 100);
        var request = new RewardRequest("Caneca", 30);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var response = service.redeemReward(request, 1L);

        verify(validator).validate(request, user);
        verify(rewardRepository).save(any(Reward.class));
        assertThat(user.getPoints()).isEqualTo(70);
        assertThat(response.remainingPoints()).isEqualTo(70);
        assertThat(response.benefits()).isEqualTo("Caneca");
    }

    @Test
    void redeemKeepsBalanceWhenValidatorFails() {
        var user = TestData.userWithPoints(1L, 10);
        var request = new RewardRequest("Caneca", 30);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new ValidationException("Pontos insuficientes")).when(validator).validate(request, user);

        assertThatThrownBy(() -> service.redeemReward(request, 1L)).isInstanceOf(ValidationException.class);

        assertThat(user.getPoints()).isEqualTo(10);
        verifyNoInteractions(rewardRepository);
    }

    @Test
    void redeemWithUnknownUserReturns404() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemReward(new RewardRequest("Caneca", 30), 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void myRewardsListsUserRewards() {
        var user = TestData.userWithPoints(1L, 5);
        when(rewardRepository.findAllByUserId(1L)).thenReturn(List.of(new Reward(1L, user, "Caneca", 30)));

        assertThat(service.myRewards(1L)).singleElement()
                .satisfies(r -> assertThat(r.pointsSpent()).isEqualTo(30));
    }
}
