package br.com.fiap.soulie.domain.user.dto;

import br.com.fiap.soulie.domain.user.User;

public record UserResponse(
        Long id,

        String name,

        String email,

        int points,

        Boolean active
) {

    public UserResponse(User user){
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPoints(),
                user.getActive()
        );
    }
}