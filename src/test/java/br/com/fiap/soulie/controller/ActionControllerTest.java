package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.action.ActionService;
import br.com.fiap.soulie.domain.action.dto.ActionRequest;
import br.com.fiap.soulie.domain.action.dto.ActionResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActionController.class)
class ActionControllerTest extends ControllerTestBase {

    @MockitoBean
    private ActionService service;

    private final ActionResponse action = new ActionResponse(1L, "Meditar", "10 minutos", 20);

    @Test
    void listActions() throws Exception {
        when(service.listActions(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(action)));

        mvc.perform(get("/action").with(logged()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Meditar"));
    }

    @Test
    void actionDetailsNotFound() throws Exception {
        when(service.actionDetails(1L)).thenThrow(new EntityNotFoundException("Ação não encontrada"));

        mvc.perform(get("/action/1").with(logged()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createActionReturns201() throws Exception {
        when(service.createAction(new ActionRequest("Meditar", "10 minutos", 20))).thenReturn(action);

        mvc.perform(post("/action").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Meditar", "description": "10 minutos", "points": 20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/action/1"))
                .andExpect(jsonPath("$.points").value(20));
    }

    @Test
    void createActionWithNegativePointsReturns400() throws Exception {
        mvc.perform(post("/action").with(logged())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Meditar", "description": "10 minutos", "points": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("points"));
        verifyNoInteractions(service);
    }
}
