package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.avatar.Avatar;
import br.com.fiap.soulie.avatar.AvatarData;
import br.com.fiap.soulie.avatar.AvatarRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avatar")
public class AvatarController {

    @Autowired
    private AvatarRepository avatarRepository;

    @PostMapping
    @Transactional
    public void save(@RequestBody @Valid AvatarData avatarData) {
        avatarRepository.save(new Avatar(avatarData));
    }


}
