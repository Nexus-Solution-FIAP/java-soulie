package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.evidence.EvidenceService;
import br.com.fiap.soulie.domain.evidence.dto.EvidenceRequest;
import br.com.fiap.soulie.domain.evidence.dto.EvidenceResponse;
import br.com.fiap.soulie.domain.validation.enums.ResultValidation;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EvidenceController.class)
class EvidenceControllerTest extends ControllerTestBase {

    @MockitoBean
    private EvidenceService service;

    @Test
    void submitEvidenceReturns201Pending() throws Exception {
        when(service.submitEvidence(new EvidenceRequest(5L, "https://img/foto.png"), 1L))
                .thenReturn(new EvidenceResponse(9L, 5L, "https://img/foto.png", ResultValidation.PENDING));

        mvc.perform(post("/evidence").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"missionId": 5, "image": "https://img/foto.png"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/evidence/mission/5"))
                .andExpect(jsonPath("$.resultValidation").value("PENDING"));
    }

    @Test
    void submitWhilePendingReturns400() throws Exception {
        when(service.submitEvidence(new EvidenceRequest(5L, "img"), 1L))
                .thenThrow(new ValidationException("Já existe uma evidência aguardando validação para esta missão"));

        mvc.perform(post("/evidence").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"missionId": 5, "image": "img"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitWithoutImageReturns400() throws Exception {
        mvc.perform(post("/evidence").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"missionId": 5, "image": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("image"));
        verifyNoInteractions(service);
    }

    @Test
    void evidencesByMission() throws Exception {
        when(service.evidencesByMission(5L, 1L))
                .thenReturn(List.of(new EvidenceResponse(9L, 5L, "img", ResultValidation.APPROVED)));

        mvc.perform(get("/evidence/mission/5").with(logged()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resultValidation").value("APPROVED"));
    }

    @Test
    void evidencesOfAnotherUsersMissionReturns403() throws Exception {
        when(service.evidencesByMission(5L, 1L)).thenThrow(new AccessDeniedException("x"));

        mvc.perform(get("/evidence/mission/5").with(logged()))
                .andExpect(status().isForbidden());
    }
}
