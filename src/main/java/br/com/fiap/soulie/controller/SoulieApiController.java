package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.SoulieResponse;
import br.com.fiap.soulie.api.dto.UpdateSoulieRequest;
import br.com.fiap.soulie.api.dto.WaterResponse;
import br.com.fiap.soulie.domain.soulie.SoulieApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/soulie", "/v1/soulie"})
@SecurityRequirement(name = "bearer-key")
public class SoulieApiController {

    @Autowired
    private SoulieApiService soulieApiService;

    @GetMapping
    public ResponseEntity<SoulieResponse> mine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(soulieApiService.mine(user));
    }

    @PatchMapping
    public ResponseEntity<SoulieResponse> update(@AuthenticationPrincipal User user,
                                                 @RequestBody UpdateSoulieRequest request) {
        return ResponseEntity.ok(soulieApiService.update(user, request));
    }

    @PostMapping("/water")
    public ResponseEntity<WaterResponse> water(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(soulieApiService.water(user));
    }
}
