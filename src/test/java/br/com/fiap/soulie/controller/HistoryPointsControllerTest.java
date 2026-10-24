package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.history_points.HistoryPointsService;
import br.com.fiap.soulie.domain.history_points.dto.HistoryPointsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoryPointsController.class)
class HistoryPointsControllerTest extends ControllerTestBase {

    @MockitoBean
    private HistoryPointsService service;

    @Test
    void myHistoryWithBrazilianDateFormat() throws Exception {
        when(service.myHistory(1L)).thenReturn(List.of(
                new HistoryPointsResponse(1L, 2L, "Meditar", LocalDate.of(2026, 10, 11), 20)));

        mvc.perform(get("/history-points").with(logged()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("11/10/2026"))
                .andExpect(jsonPath("$[0].pointsEarned").value(20));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        // Sem credencial o correto e 401, nao 403.
        mvc.perform(get("/history-points")).andExpect(status().isUnauthorized());
    }
}
