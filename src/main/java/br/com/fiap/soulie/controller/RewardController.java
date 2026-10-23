package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.reward.RewardService;
import br.com.fiap.soulie.domain.reward.dto.RewardCatalogResponse;
import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.reward.dto.RewardResponse;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reward")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Recompensas", description = "Catálogo de recompensas e resgates do usuário")
public class RewardController {

    @Autowired
    private RewardService service;

    @GetMapping
    @Operation(summary = "Meus resgates", description = "Histórico de recompensas que você já resgatou.")
    public ResponseEntity<List<RewardResponse>> myRewards(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.myRewards(user.getId()));
    }

    @GetMapping("/catalog")
    @Operation(summary = "Recompensas disponíveis",
            description = "Lista o que pode ser resgatado e quanto custa. Use o id daqui no POST /reward.")
    public ResponseEntity<List<RewardCatalogResponse>> catalog() {
        return ResponseEntity.ok(service.catalog());
    }

    @PostMapping
    @Operation(summary = "Resgata uma recompensa",
            description = "Informe apenas o id do item do catálogo: o custo é definido pelo servidor. Responde 409 se o saldo não cobrir.")
    public ResponseEntity<RewardResponse> redeemReward(
            @RequestBody @Valid RewardRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.redeemReward(request, user.getId()));
    }
}
