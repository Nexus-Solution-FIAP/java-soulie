package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.AchievementResponse;
import br.com.fiap.soulie.domain.achievement.AchievementApiService;
import br.com.fiap.soulie.domain.soulie.SoulieApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/achievements", "/v1/achievements"})
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Conquistas", description = "Selos e o progresso de cada um, inclusive a sequência de 7 dias")
public class AchievementController {

    @Autowired
    private AchievementApiService achievementApiService;

    @Autowired
    private SoulieApiService soulieApiService;

    @GetMapping
    @Operation(summary = "Lista as conquistas", description = "Recalcula o progresso antes de responder. Chama Constante destrava com 7 dias de sequência.")
    public ResponseEntity<List<AchievementResponse>> list(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        var avatar = soulieApiService.getOrCreate(user);
        return ResponseEntity.ok(achievementApiService.list(user, avatar));
    }
}
