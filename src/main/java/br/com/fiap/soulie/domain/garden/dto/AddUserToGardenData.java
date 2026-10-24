package br.com.fiap.soulie.domain.garden.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddUserToGardenData(
        @NotNull(message = "Precisa selecionar o Jardim")
        Long gardenId,

        @NotNull(message = "Precisa ter ao menos um usuário")
        List<Long> usersId
) {
}
