package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.user.ListUserData;
import br.com.fiap.soulie.user.User;
import br.com.fiap.soulie.user.UserData;
import br.com.fiap.soulie.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired // Injeção de depencia - Aqui ele instancia automaticamente
    private UserRepository userRepository;

    @PostMapping
    @Transactional
    public void addUser(@RequestBody @Valid UserData userData) {
        userRepository.save(new User(userData));
    }

    @GetMapping
    public List<ListUserData> listUsers() {
        return userRepository.findAll().stream().map(ListUserData:: new).toList();
    }
}
