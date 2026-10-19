package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.ActivityResponse;
import br.com.fiap.soulie.api.dto.CreateActivityRequest;
import br.com.fiap.soulie.api.dto.UpdateSettingsRequest;
import br.com.fiap.soulie.api.dto.UserProfileResponse;
import br.com.fiap.soulie.domain.activity.ActivityApiService;
import br.com.fiap.soulie.domain.profile.ProfileApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/users/me", "/v1/users/me"})
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Perfil", description = "Dados do usuário logado, sequência, ajustes e histórico")
public class MeController {

    @Autowired
    private ProfileApiService profileApiService;

    @Autowired
    private ActivityApiService activityApiService;

    @GetMapping
    @Operation(summary = "Perfil logado", description = "Nome, sequência de dias, XP, ranking e impacto. streakDays vale o dia de hoje ou de ontem.")
    public ResponseEntity<UserProfileResponse> me(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileApiService.me(user));
    }

    @PatchMapping("/settings")
    @Operation(summary = "Atualiza avisos", description = "Liga ou desliga notificação no app e o resumo semanal.")
    public ResponseEntity<UserProfileResponse> settings(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                        @RequestBody UpdateSettingsRequest request) {
        return ResponseEntity.ok(profileApiService.updateSettings(user, request));
    }

    @GetMapping("/activities")
    @Operation(summary = "Histórico de ações")
    public ResponseEntity<List<ActivityResponse>> activities(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(activityApiService.list(user));
    }

    @PostMapping("/activities")
    @Operation(summary = "Registra uma ação", description = "Se o texto contiver o nome de uma missão do dia ainda aberta, ela é concluída e a sequência do dia sobe uma vez.")
    public ResponseEntity<ActivityResponse> addActivity(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                        @RequestBody CreateActivityRequest request) {
        return ResponseEntity.ok(activityApiService.create(user, request));
    }
}
