package br.com.fiap.soulie.domain.user.dto;

public record UpdateUserData(

        String name,

        String email,

        String password
) {
}
