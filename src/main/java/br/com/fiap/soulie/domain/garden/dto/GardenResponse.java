package br.com.fiap.soulie.domain.garden.dto;

import br.com.fiap.soulie.domain.user.dto.UserResponse;

import java.util.List;

public record GardenResponse(

        Long id,

        String name,

        List<UserResponse> users
) {
}
