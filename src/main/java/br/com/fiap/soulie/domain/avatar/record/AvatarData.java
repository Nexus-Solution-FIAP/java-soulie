package br.com.fiap.soulie.domain.avatar.record;

import br.com.fiap.soulie.domain.avatar.enums.PlantType;
import jakarta.validation.constraints.NotBlank;

public record AvatarData(

        @NotBlank
        String name,

        // @NotNull
        Long userId,

        PlantType plantType
) {
}
