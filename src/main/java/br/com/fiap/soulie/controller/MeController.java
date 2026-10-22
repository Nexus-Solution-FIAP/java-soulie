package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.ActivityResponse;
import br.com.fiap.soulie.api.dto.CreateActivityRequest;
import br.com.fiap.soulie.api.dto.RegisterPushTokenRequest;
import br.com.fiap.soulie.api.dto.UpdateSettingsRequest;
import br.com.fiap.soulie.api.dto.UserDataExportResponse;
import br.com.fiap.soulie.api.dto.UserProfileResponse;
import br.com.fiap.soulie.domain.activity.ActivityApiService;
import br.com.fiap.soulie.domain.profile.ProfileApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @GetMapping("/export")
    @Operation(summary = "Exporta os dados da conta", description = "Perfil, atividades e conquistas do usuário logado.")
    public ResponseEntity<UserDataExportResponse> exportData(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileApiService.exportData(user));
    }

    @DeleteMapping
    @Operation(summary = "Exclui a conta", description = "Desativa o usuário, sai do jardim e para de receber push.")
    @ApiResponse(responseCode = "204", description = "Conta desativada")
    public ResponseEntity<Void> deleteAccount(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        profileApiService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/settings")
    @Operation(summary = "Atualiza avisos", description = "Liga ou desliga notificação no app, resumo semanal, cutucadas e nome no jardim.")
    public ResponseEntity<UserProfileResponse> settings(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                        @RequestBody @Valid UpdateSettingsRequest request) {
        return ResponseEntity.ok(profileApiService.updateSettings(user, request));
    }

    @PutMapping("/push-token")
    @Operation(summary = "Registra o token de push", description = "Guarda o Expo Push Token do aparelho para avisar cutucadas com o app fechado.")
    public ResponseEntity<Void> registerPushToken(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                  @RequestBody @Valid RegisterPushTokenRequest request) {
        profileApiService.registerPushToken(user, request == null ? null : request.token());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/push-token")
    @Operation(summary = "Remove o token de push")
    public ResponseEntity<Void> clearPushToken(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        profileApiService.registerPushToken(user, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activities")
    @Operation(summary = "Histórico de ações")
    public ResponseEntity<List<ActivityResponse>> activities(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(activityApiService.list(user));
    }

    @PostMapping("/activities")
    @Operation(summary = "Registra uma ação livre",
            description = """
                    Registra uma atividade ecológica no histórico. O XP é opcional e limitado a 50.

                    NÃO conclui missão do catálogo: desde que existe validação por IA, missão só \
                    se conclui por POST /v1/missions/{id}/validate, com foto. Se o texto mencionar \
                    o nome de uma missão, a requisição é recusada com orientação.""")
    public ResponseEntity<ActivityResponse> addActivity(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                        // @Valid é obrigatório: sem ele o @Max do
                                                        // DTO não roda, e o XP vindo do cliente
                                                        // voltaria a entrar sem teto.
                                                        @RequestBody @Valid CreateActivityRequest request) {
        return ResponseEntity.ok(activityApiService.create(user, request));
    }
}
