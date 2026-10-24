package br.com.fiap.soulie.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Convite de 6 caracteres gerado ao abrir um jardim")
public record JoinGardenRequest(
        @JsonAlias({"inviteCode", "invite_code"})
        @Schema(example = "AB12CD") String code
) {
}
