package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.reward.RewardService;
import br.com.fiap.soulie.domain.reward.dto.RewardRequest;
import br.com.fiap.soulie.domain.reward.dto.RewardResponse;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reward")
@SecurityRequirement(name = "bearer-key")
public class RewardController {

    @Autowired
    private RewardService service;

    @GetMapping
    public ResponseEntity<List<RewardResponse>> myRewards(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.myRewards(user.getId()));
    }

    @PostMapping
    public ResponseEntity<RewardResponse> redeemReward(
            @RequestBody @Valid RewardRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.redeemReward(request, user.getId()));
    }
}
