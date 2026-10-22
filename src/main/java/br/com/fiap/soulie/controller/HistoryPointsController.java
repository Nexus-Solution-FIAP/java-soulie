package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.history_points.HistoryPointsService;
import br.com.fiap.soulie.domain.history_points.dto.HistoryPointsResponse;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/history-points")
@SecurityRequirement(name = "bearer-key")
public class HistoryPointsController {

    @Autowired
    private HistoryPointsService service;

    @GetMapping
    public ResponseEntity<List<HistoryPointsResponse>> myHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.myHistory(user.getId()));
    }
}
