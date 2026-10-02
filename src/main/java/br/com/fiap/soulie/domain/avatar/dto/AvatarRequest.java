package br.com.fiap.soulie.domain.avatar.dto;

import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import jakarta.validation.constraints.NotBlank;

public record AvatarRequest(

        @NotBlank
        String name,

        PlantType plantType
) {
}
