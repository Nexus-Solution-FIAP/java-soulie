package br.com.fiap.soulie.domain.avatar.dto;

import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.enums.Humor;
import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import br.com.fiap.soulie.domain.user.dto.UserResponse;

public record AvatarResponse(

        Long idSoulie,

        String nameSoulie,

        int level,

        Humor humor,

        PlantType plantType,

        UserResponse user

) {
}
