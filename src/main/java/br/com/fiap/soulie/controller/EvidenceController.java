package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.evidence.EvidenceService;
import br.com.fiap.soulie.domain.evidence.dto.EvidenceRequest;
import br.com.fiap.soulie.domain.evidence.dto.EvidenceResponse;
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
@RequestMapping("/evidence")
@SecurityRequirement(name = "bearer-key")
public class EvidenceController {

    @Autowired
    private EvidenceService service;

    @GetMapping("/mission/{missionId}")
    public ResponseEntity<List<EvidenceResponse>> evidencesByMission(
            @PathVariable Long missionId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.evidencesByMission(missionId, user.getId()));
    }

    @PostMapping
    public ResponseEntity<EvidenceResponse> submitEvidence(
            @RequestBody @Valid EvidenceRequest request,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder uriBuilder) {
        var detail = service.submitEvidence(request, user.getId());
        var uri = uriBuilder.path("/evidence/mission/{missionId}").buildAndExpand(detail.missionId()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }
}
