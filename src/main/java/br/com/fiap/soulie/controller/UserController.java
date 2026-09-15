package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.database.repository.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("users")
public class UserController {

    @GetMapping
    public List<UserEntity> getUsers() {
        List<UserEntity> users = new ArrayList<>();

        return users;
    }
}
