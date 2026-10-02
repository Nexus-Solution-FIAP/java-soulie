package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserService;
import br.com.fiap.soulie.domain.user.dto.UpdateUserData;
import br.com.fiap.soulie.domain.user.dto.UserRequest;
import br.com.fiap.soulie.domain.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/user")
@SecurityRequirement(name = "bearer-key")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> userDetails(@PathVariable Long id) {
        return ResponseEntity.ok(service.userDetails(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsersDetails(
            @PageableDefault(size = 5, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(service.listUsersDetails(pageable));
    }

    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@RequestBody @Valid UserRequest userRequest,
                                                     UriComponentsBuilder uriBuilder) {
        var detail = service.registerUser(userRequest);
        var uri = uriBuilder.path("/user/{id}").buildAndExpand(detail.id()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateUser(@RequestBody @Valid UpdateUserData updateUserData,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.updateUser(updateUserData, user.getId()));

    }

    @PatchMapping("{id}")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(service.activeUser(id));
    }

    @DeleteMapping
    public ResponseEntity<UserResponse> excludeUser(@PathVariable Long id) {
        service.excludeUser(id);
        return ResponseEntity.noContent().build();
    }
}
