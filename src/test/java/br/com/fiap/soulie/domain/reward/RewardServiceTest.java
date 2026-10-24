package br.com.fiap.soulie.domain.reward;

import br.com.fiap.soulie.TestData;
import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.exception.BusinessException;
import br.com.fiap.soulie.validations.reward.RewardRedeemValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock private RewardRepository rewardRepository;
    @Mock private RewardCatalogRepository catalogRepository;
    @Mock private UserRepository userRepository;
    @Mock private RewardRedeemValidator validator;

    @InjectMocks
    private RewardService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "redeemValidators", List.of(validator));
    }

    @Test
    @DisplayName("O custo e o texto vêm do catálogo, nunca da requisição")
    void redeemUsesCatalogCostNotClientInput() {
        var user = TestData.userWithPoints(1L, 100);
        var item = TestData.rewardCatalog(5L, "Vaso decorado", 250);
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.debitPoints(1L, 250)).thenReturn(1);
        when(rewardRepository.save(any(Reward.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.redeemReward(new RewardRequest(5L), 1L);

        // O debito usa o custo do CATALOGO (250), nao um numero do cliente.
        verify(userRepository).debitPoints(1L, 250);
        assertThat(response.benefits()).isEqualTo("Vaso decorado");
        assertThat(response.pointsSpent()).isEqualTo(250);
    }

    @Test
    @DisplayName("Se o débito atômico não afeta linha, o resgate é abortado com 409")
    void redeemAbortsWhenAtomicDebitFindsNoBalance() {
        var user = TestData.userWithPoints(1L, 100);
        var item = TestData.rewardCatalog(5L, "Brinde", 100);
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // Simula a corrida: o saldo passou pelo validator mas sumiu antes do debito.
        when(userRepository.debitPoints(1L, 100)).thenReturn(0);

        assertThatThrownBy(() -> service.redeemReward(new RewardRequest(5L), 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(rewardRepository, never()).save(any());
    }

    @Test
    void redeemKeepsBalanceWhenValidatorFails() {
        var user = TestData.userWithPoints(1L, 10);
        var item = TestData.rewardCatalog(5L, "Brinde", 30);
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new ValidationException("Pontos insuficientes")).when(validator).validate(item, user);

        assertThatThrownBy(() -> service.redeemReward(new RewardRequest(5L), 1L))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).debitPoints(anyLong(), anyInt());
        verifyNoInteractions(rewardRepository);
    }

    @Test
    void redeemWithUnknownCatalogItemReturns404() {
        when(catalogRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeemReward(new RewardRequest(9L), 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Item inativo não pode ser resgatado")
    void redeemRejectsInactiveCatalogItem() {
        var item = RewardCatalog.builder().id(5L).benefits("Antigo").cost(10).active(false).build();
        when(catalogRepository.findById(5L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.redeemReward(new RewardRequest(5L), 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Esta recompensa não está mais disponível");

        verifyNoInteractions(rewardRepository);
    }

    @Test
    void myRewardsListsUserRewards() {
        var user = TestData.userWithPoints(1L, 5);
        var item = TestData.rewardCatalog(1L, "Caneca", 30);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rewardRepository.findAllByUserId(1L)).thenReturn(List.of(new Reward(item, user)));

        assertThat(service.myRewards(1L)).singleElement()
                .satisfies(r -> assertThat(r.pointsSpent()).isEqualTo(30));
    }

    @Test
    void catalogListsOnlyActiveItems() {
        when(catalogRepository.findAllByActiveTrueOrderByCostAsc())
                .thenReturn(List.of(TestData.rewardCatalog(1L, "Semente", 100)));

        assertThat(service.catalog()).singleElement()
                .satisfies(c -> assertThat(c.cost()).isEqualTo(100));
    }
}
