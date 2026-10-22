package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.reward.RewardService;
import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.reward.dto.RewardResponse;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RewardController.class)
class RewardControllerTest extends ControllerTestBase {

    @MockitoBean
    private RewardService service;

    @Test
    void redeemReturns201WithRemainingPoints() throws Exception {
        when(service.redeemReward(new RewardRequest("Caneca", 30), 1L))
                .thenReturn(new RewardResponse(1L, "Caneca", 30, 70));

        mvc.perform(post("/reward").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"benefits": "Caneca", "pointsSpent": 30}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingPoints").value(70));
    }

    @Test
    void redeemWithoutPointsReturns400() throws Exception {
        when(service.redeemReward(new RewardRequest("Caneca", 30), 1L))
                .thenThrow(new ValidationException("Pontos insuficientes para resgatar esta recompensa"));

        mvc.perform(post("/reward").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"benefits": "Caneca", "pointsSpent": 30}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Pontos insuficientes para resgatar esta recompensa"));
    }

    @Test
    void redeemWithZeroPointsReturns400() throws Exception {
        mvc.perform(post("/reward").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"benefits": "Caneca", "pointsSpent": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("pointsSpent"));
        verifyNoInteractions(service);
    }

    @Test
    void myRewards() throws Exception {
        when(service.myRewards(1L)).thenReturn(List.of(new RewardResponse(1L, "Caneca", 30, 70)));

        mvc.perform(get("/reward").with(logged()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].benefits").value("Caneca"));
    }
}
