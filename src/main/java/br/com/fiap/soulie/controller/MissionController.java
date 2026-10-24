package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.mission.MissionService;
import br.com.fiap.soulie.domain.mission.dto.MissionRequest;
import br.com.fiap.soulie.domain.mission.dto.MissionResponse;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/mission")
@SecurityRequirement(name = "bearer-key")
public class MissionController {

    @Autowired
    private MissionService service;

    @GetMapping
    public ResponseEntity<List<MissionResponse>> myMissions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.myMissions(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MissionResponse> missionDetails(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.missionDetails(id, user.getId()));
    }

    @PostMapping
    public ResponseEntity<MissionResponse> createMission(
            @RequestBody @Valid MissionRequest request,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder uriBuilder) {
        var detail = service.createMission(request, user.getId());
        var uri = uriBuilder.path("/mission/{id}").buildAndExpand(detail.id()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<AvatarResponse> completeMission(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.completeMission(id, user.getId()));
    }
}
