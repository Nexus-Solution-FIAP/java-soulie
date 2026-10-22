package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.SoulieResponse;
import br.com.fiap.soulie.api.dto.UpdateSoulieRequest;
import br.com.fiap.soulie.api.dto.WaterResponse;
import br.com.fiap.soulie.domain.soulie.SoulieApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/soulie", "/v1/soulie"})
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Soulie", description = "Planta do usuário: nome, flor, nível e rega")
public class SoulieApiController {

    @Autowired
    private SoulieApiService soulieApiService;

    @GetMapping
    @Operation(summary = "Meu Soulie")
    public ResponseEntity<SoulieResponse> mine(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(soulieApiService.mine(user));
    }

    @PatchMapping
    @Operation(summary = "Renomeia ou troca a flor", description = "Flores: girassol, rosa, hibisco, lavanda, margarida, orquidea.")
    public ResponseEntity<SoulieResponse> update(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                 @RequestBody @Valid UpdateSoulieRequest request) {
        return ResponseEntity.ok(soulieApiService.update(user, request));
    }

    @PostMapping("/water")
    @Operation(summary = "Rega o Soulie", description = "A primeira rega do dia conta 1 na sequência. Regar de novo no mesmo dia devolve a sequência atual sem somar.")
    public ResponseEntity<WaterResponse> water(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(soulieApiService.water(user));
    }
}
