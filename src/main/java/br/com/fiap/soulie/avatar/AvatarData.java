package br.com.fiap.soulie.avatar;

import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AvatarData(
        @NotNull
        long idAvatar,

        @NotNull
        long idUser,

        @NotBlank
        String name,

        @Negative
        int level
) {
}
