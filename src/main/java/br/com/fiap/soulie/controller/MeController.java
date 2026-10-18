package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.ActivityResponse;
import br.com.fiap.soulie.api.dto.CreateActivityRequest;
import br.com.fiap.soulie.api.dto.UpdateSettingsRequest;
import br.com.fiap.soulie.api.dto.UserProfileResponse;
import br.com.fiap.soulie.domain.activity.ActivityApiService;
import br.com.fiap.soulie.domain.profile.ProfileApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/users/me", "/v1/users/me"})
@SecurityRequirement(name = "bearer-key")
public class MeController {

    @Autowired
    private ProfileApiService profileApiService;

    @Autowired
    private ActivityApiService activityApiService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileApiService.me(user));
    }

    @PatchMapping("/settings")
    public ResponseEntity<UserProfileResponse> settings(@AuthenticationPrincipal User user,
                                                        @RequestBody UpdateSettingsRequest request) {
        return ResponseEntity.ok(profileApiService.updateSettings(user, request));
    }

    @GetMapping("/activities")
    public ResponseEntity<List<ActivityResponse>> activities(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(activityApiService.list(user));
    }

    @PostMapping("/activities")
    public ResponseEntity<ActivityResponse> addActivity(@AuthenticationPrincipal User user,
                                                        @RequestBody CreateActivityRequest request) {
        return ResponseEntity.ok(activityApiService.create(user, request));
    }
}
