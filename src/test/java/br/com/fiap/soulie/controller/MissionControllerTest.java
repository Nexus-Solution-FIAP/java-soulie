package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.mission.MissionService;
import br.com.fiap.soulie.domain.mission.enums.MissionStatus;
import br.com.fiap.soulie.domain.mission.dto.MissionRequest;
import br.com.fiap.soulie.domain.mission.dto.MissionResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MissionController.class)
class MissionControllerTest extends ControllerTestBase {

    @MockitoBean
    private MissionService service;

    private final MissionResponse response =
            new MissionResponse(5L, 2L, "Beber água", 40, "2 litros", MissionStatus.ACTIVE);

    @Test
    void createMissionReturns201WithLocation() throws Exception {
        when(service.createMission(new MissionRequest(2L, "2 litros"), 1L)).thenReturn(response);

        mvc.perform(post("/mission").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actionId": 2, "description": "2 litros"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/mission/5"))
                .andExpect(jsonPath("$.actionName").value("Beber água"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createMissionWithInvalidBodyReturns400() throws Exception {
        mvc.perform(post("/mission").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[*].field").value(org.hamcrest.Matchers.containsInAnyOrder("actionId", "description")));
        verifyNoInteractions(service);
    }

    @Test
    void createMissionWithUnknownActionReturns404() throws Exception {
        when(service.createMission(any(), eq(1L))).thenThrow(new EntityNotFoundException("Ação não encontrada"));

        mvc.perform(post("/mission").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actionId": 99, "description": "x"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void listMyMissions() throws Exception {
        when(service.myMissions(1L)).thenReturn(List.of(response));

        mvc.perform(get("/mission").with(logged()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    void missionOfAnotherUserReturns403() throws Exception {
        when(service.missionDetails(5L, 1L)).thenThrow(new AccessDeniedException("x"));

        mvc.perform(get("/mission/5").with(logged()))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeMissionReturnsUpdatedAvatar() throws Exception {
        var avatar = new AvatarResponse(1L, "Soulie", 2, Humor.NEUTRAL, PlantType.IPE_AMARELO, userResponse(1L));
        when(service.completeMission(5L, 1L)).thenReturn(avatar);

        mvc.perform(put("/mission/5/complete").with(logged()))
                .andExpect(status().isOk())
                // O AvatarResponse do java-soulie nao expoe `experience`: o nivel
                // ja e derivado do xp total pelo SoulieLevel.
                .andExpect(jsonPath("$.level").value(2));
    }

    @Test
    void completeMissionWithoutApprovalReturns400WithMessage() throws Exception {
        when(service.completeMission(5L, 1L))
                .thenThrow(new ValidationException("A missão ainda não possui evidência aprovada"));

        mvc.perform(put("/mission/5/complete").with(logged()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("A missão ainda não possui evidência aprovada"));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mvc.perform(get("/mission"))
                // Sem credencial o correto e 401, nao 403: o RestAuthenticationEntryPoint
                // agora responde pela requisicao nao autenticada.
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
