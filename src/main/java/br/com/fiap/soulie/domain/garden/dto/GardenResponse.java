package br.com.fiap.soulie.domain.garden.dto;

import br.com.fiap.soulie.domain.user.dto.UserResponse;

import java.util.List;

public record GardenResponse(

        Long id,

        String name,

        String inviteCode,

        List<UserResponse> users
) {
}
