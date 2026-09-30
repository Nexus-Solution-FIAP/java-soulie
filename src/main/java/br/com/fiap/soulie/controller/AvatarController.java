package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.avatar.AvatarService;
import br.com.fiap.soulie.domain.avatar.record.AvatarData;
import br.com.fiap.soulie.domain.avatar.record.AvatarDataDetail;
import br.com.fiap.soulie.domain.avatar.record.UpdateAvatar;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/avatar")
public class AvatarController {

    @Autowired
    private AvatarService service;

    @GetMapping
    public ResponseEntity<AvatarDataDetail> myAvatar(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(service.avatarDetails(user.getId()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AvatarDataDetail> avatarDetails(@PathVariable Long userId){
        return ResponseEntity.ok(service.avatarDetails(userId));
    }

    @PostMapping
    public ResponseEntity<AvatarDataDetail> createMyAvatar(
            @RequestBody @Valid AvatarData avatarData,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder uriBuilder
    ){
        var detail = service.createMyAvatar(avatarData, user.getId());
        var uri = uriBuilder.path("/avatar/{id}").buildAndExpand(detail.idSoulie()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }

    @PutMapping
    public ResponseEntity<AvatarDataDetail> updateMyAvatar(
            @RequestBody @Valid UpdateAvatar updateAvatar,
            @AuthenticationPrincipal User user
    ){
        return ResponseEntity.ok(service.updateMyAvatar(updateAvatar, user.getId()));
    }
}
