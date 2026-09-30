package br.com.fiap.soulie.domain.user.record;

import br.com.fiap.soulie.domain.user.User;

public record UserDataDetail(
        Long id,

        String name,

        String email,

        int points,

        Boolean active
) {

    public UserDataDetail(User user){
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPoints(),
                user.getActive()
        );
    }
}