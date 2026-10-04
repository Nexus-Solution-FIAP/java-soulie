package br.com.fiap.soulie.domain.garden.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record GardenRequest(

        @NotBlank
        String name,

        List<Long> usersId
) {
}
