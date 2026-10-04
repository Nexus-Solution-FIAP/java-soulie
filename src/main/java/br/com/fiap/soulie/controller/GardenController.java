package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.garden.GardenService;
import br.com.fiap.soulie.domain.garden.dto.AddUserToGardenData;
import br.com.fiap.soulie.domain.garden.dto.GardenRequest;
import br.com.fiap.soulie.domain.garden.dto.GardenResponse;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/garden")
@SecurityRequirement(name = "bearer-key")
public class GardenController {

    @Autowired
    private GardenService service;

    @GetMapping
    public ResponseEntity<List<GardenResponse>> listMyGardens(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.myGardens(user.getId()));
    }

    @GetMapping("{id}")
    public ResponseEntity<GardenResponse> gardenDetails(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.gardenDetails(id, user.getId()));
    }

    @GetMapping("{id}/avatars")
    public ResponseEntity<List<AvatarResponse>> avatars(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.gardenAvatarDetails(id, user.getId()));
    }

    @PostMapping
    public ResponseEntity<GardenResponse> createGarden(
            @RequestBody GardenRequest request, @AuthenticationPrincipal User user, UriComponentsBuilder builder) {
        var detail = service.createGarden(request, user.getId());
        var uri = builder.path("/garden/{id}").buildAndExpand(detail.id()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }

    @PatchMapping
    public ResponseEntity<GardenResponse> addUserToGarden(@RequestBody AddUserToGardenData data, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.addUserToGarden(data, user.getId()));
    }
}
