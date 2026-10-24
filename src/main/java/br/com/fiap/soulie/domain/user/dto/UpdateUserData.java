package br.com.fiap.soulie.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Atualização da própria conta. Todos os campos são opcionais.
 *
 * <p>As anotações repetem a política do {@code UserRequest} de propósito. Este
 * record não tinha validação nenhuma, então o cadastro exigia 8 caracteres com
 * maiúscula, minúscula, dígito e símbolo, mas a atualização aceitava
 * {@code "a"} — a política de senha tinha um desvio trivial. E o e-mail, que é
 * o login e o subject do JWT, podia virar um valor que não é endereço nenhum,
 * deixando a conta sem caminho de recuperação.
 */
public record UpdateUserData(

        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        @Schema(example = "Ana Silva")
        String name,

        @Email(message = "Formato do email é inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        @Schema(example = "ana@exemplo.com")
        String email,

        // Idêntico ao do UserRequest, de propósito: duas expressões diferentes
        // viram duas políticas diferentes na primeira vez que alguém ajustar uma
        // e esquecer a outra.
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Formato da senha inválida"
        )
        @Schema(example = "Soulie@2026")
        String password
) {
}
