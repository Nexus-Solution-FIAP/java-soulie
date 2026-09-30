package br.com.fiap.soulie.domain.avatar.record;

import br.com.fiap.soulie.domain.avatar.enums.PlantType;

public record UpdateAvatar(

        String name,

        PlantType plantType
) {
}
