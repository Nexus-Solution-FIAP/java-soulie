package br.com.fiap.soulie.domain.user.record;

import jakarta.validation.constraints.NotNull;

public record UpdateUserData(

        @NotNull
        Long id,

        String name,

        String email,

        String password
) {
}
