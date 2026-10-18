package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.DailyMissionResponse;
import br.com.fiap.soulie.api.dto.MissionValidationResponse;
import br.com.fiap.soulie.domain.mission.MissionApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class MissionApiController {

    @Autowired
    private MissionApiService missionApiService;

    @GetMapping("/daily")
    public ResponseEntity<List<DailyMissionResponse>> daily(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(missionApiService.daily(user));
    }

    @PostMapping(value = "/{id}/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MissionValidationResponse> validate(@AuthenticationPrincipal User user,
                                                              @PathVariable Long id,
                                                              @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.ok(missionApiService.validate(user, id, photo));
    }
}
