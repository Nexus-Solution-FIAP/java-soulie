package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.DailyMissionResponse;
import br.com.fiap.soulie.api.dto.MissionValidationResponse;
import br.com.fiap.soulie.domain.mission.MissionApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/missions", "/v1/missions"})
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Missões", description = "Missões diárias, semanais, mensais e anuais")
public class MissionApiController {

    @Autowired
    private MissionApiService missionApiService;

    @GetMapping
    @Operation(summary = "Missões do período", description = "Lista diárias, semanais, mensais e anuais. O campo period diz a frequência; done vale só na janela atual (hoje, semana, mês ou ano).")
    public ResponseEntity<List<DailyMissionResponse>> list(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(missionApiService.list(user));
    }

    @GetMapping("/daily")
    @Operation(summary = "Missões (alias)", description = "Mesmo conteúdo de GET /missions, incluindo todos os períodos.")
    public ResponseEntity<List<DailyMissionResponse>> daily(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(missionApiService.list(user));
    }

    @PostMapping(value = "/{id}/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia a foto da missão para validação",
            description = """
                    A foto agora é OBRIGATÓRIA e é analisada por IA antes de valer XP.

                    Responde 202: a análise acontece em segundo plano. Guarde o `validationId` e                     consulte GET /v1/missions/validations/{id}, ou espere a notificação push —                     ela chega quando o resultado sai.

                    Enquanto o status for PENDING, `xpGained` vem 0 e `success` vem false.                     Diárias só uma vez por dia; semanais, mensais e anuais só uma vez no período.""")
    public ResponseEntity<MissionValidationResponse> validate(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "Id da missão do dia", example = "2") @PathVariable Long id,
            @Parameter(description = "Foto que comprova a ação. Obrigatória.") @RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.accepted().body(missionApiService.validate(user, id, photo));
    }

    @GetMapping("/validations/{id}")
    @Operation(summary = "Status de uma validação",
            description = """
                    Consulta o resultado da análise da foto. Enquanto `status` for PENDING,                     a IA ainda não respondeu.

                    APPROVED: o XP já foi creditado e os campos de recompensa estão preenchidos.                     REJECTED: a foto não comprova a missão; o motivo vem em `aiDetections`.                     FAILED: não foi possível analisar — é falha nossa, a evidência será reprocessada.""")
    public ResponseEntity<MissionValidationResponse> validationStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "Id devolvido no envio da foto", example = "42") @PathVariable Long id) {
        return ResponseEntity.ok(missionApiService.statusDaValidacao(user, id));
    }
}
