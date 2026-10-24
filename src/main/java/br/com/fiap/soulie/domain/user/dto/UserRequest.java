package br.com.fiap.soulie.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Cadastro público. A senha precisa de 8 caracteres, com maiúscula, minúscula, número e um símbolo @$!%*?&.")
public record UserRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Schema(example = "Arthur Silva")
        String name,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Formato do email é inválido")
        @Schema(example = "arthur@email.com")
        String email,

        @NotBlank(message = "Senha é obrigatório")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Formato da senha inválida")
        @Schema(example = "Soulie@2026")
        String password
) {
}
