package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.domain.user.UserService;
import br.com.fiap.soulie.domain.user.record.UpdateUserData;
import br.com.fiap.soulie.domain.user.record.UserData;
import br.com.fiap.soulie.domain.user.record.UserDataDetail;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("/{id}")
    public ResponseEntity<UserDataDetail> userDetails(@PathVariable Long id) {
        return ResponseEntity.ok(service.userDetails(id));
    }

    @GetMapping
    public ResponseEntity<Page<UserDataDetail>> listUsersDetails(
            @PageableDefault(size = 5, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(service.listUsersDetails(pageable));
    }

    @PostMapping
    public ResponseEntity<UserDataDetail> registerUser(@RequestBody @Valid UserData userData,
                                                       UriComponentsBuilder uriBuilder) {
        var detail = service.registerUser(userData);
        var uri = uriBuilder.path("/user/{id}").buildAndExpand(detail.id()).toUri();
        return ResponseEntity.created(uri).body(detail);
    }

//    @PostMapping("/batch")
//    public ResponseEntity<List<UserDataDetail>> createUsers(@RequestBody List<@Valid UserData> usersData,
//                                                            UriComponentsBuilder uriBuilder){
//        var users = service.createUsers(usersData);
//        var uri = uriBuilder.path("/user").build().toUri();
//        return ResponseEntity.created(uri).body(users);
//    }

    @PutMapping
    public ResponseEntity<UserDataDetail> updateUser(@RequestBody @Valid UpdateUserData updateUserData) {
        return ResponseEntity.ok(service.updateUser(updateUserData));

    }

    @PatchMapping("{id}")
    public ResponseEntity<UserDataDetail> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(service.activeUser(id));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<UserDataDetail> excludeUser(@PathVariable Long id) {
        service.excludeUser(id);
        return ResponseEntity.noContent().build();
    }
}
