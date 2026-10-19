package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.avatar.AvatarService;
import br.com.fiap.soulie.domain.avatar.dto.AvatarRequest;
import br.com.fiap.soulie.domain.avatar.dto.AvatarResponse;
import br.com.fiap.soulie.domain.avatar.dto.UpdateAvatar;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/avatar")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Avatar (legado)", description = "Contrato antigo. O app usa o grupo Soulie.")
public class AvatarController {

    @Autowired
    private AvatarService service;

    @GetMapping
    public ResponseEntity<AvatarResponse> myAvatar(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(service.avatarDetails(user.getId()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AvatarResponse> avatarDetails(@PathVariable Long userId){
        return ResponseEntity.ok(service.avatarDetails(userId));
    }

    @PostMapping
    public ResponseEntity<AvatarResponse> createMyAvatar(
            @RequestBody @Valid AvatarRequest avatarRequest,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder uriBuilder
    ){
        var detail = service.createMyAvatar(avatarRequest, user.getId());
        var uri = uriBuilder.path("/avatar/{id}").buildAndExpand(detail.idSoulie()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }

    @PutMapping
    public ResponseEntity<AvatarResponse> updateMyAvatar(
            @RequestBody @Valid UpdateAvatar updateAvatar,
            @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(service.updateMyAvatar(updateAvatar, user.getId()));
    }
}
