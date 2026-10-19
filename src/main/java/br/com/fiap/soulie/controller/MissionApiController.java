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
@Tag(name = "Missões", description = "Missões do dia e validação com foto")
public class MissionApiController {

    @Autowired
    private MissionApiService missionApiService;

    @GetMapping("/daily")
    @Operation(summary = "Missões de hoje", description = "Lista as missões e marca as que o usuário já concluiu nesta data.")
    public ResponseEntity<List<DailyMissionResponse>> daily(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(missionApiService.daily(user));
    }

    @PostMapping(value = "/{id}/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Conclui a missão com foto", description = "A foto é opcional. A primeira conclusão do dia soma 1 na sequência e devolve o XP da missão. Repetir a mesma missão no mesmo dia não soma de novo.")
    public ResponseEntity<MissionValidationResponse> validate(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                              @Parameter(description = "Id da missão do dia", example = "2") @PathVariable Long id,
                                                              @Parameter(description = "Foto da ação. Pode ir vazio.") @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.ok(missionApiService.validate(user, id, photo));
    }
}
