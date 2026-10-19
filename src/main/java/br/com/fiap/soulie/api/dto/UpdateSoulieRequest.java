package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nome e flor do Soulie. Envie só o que for mudar.")
public record UpdateSoulieRequest(
        @Schema(example = "Soulie da Sorte") String name,
        @Schema(example = "girassol", description = "girassol, rosa, hibisco, lavanda, margarida ou orquidea") String flowerType
) {
}
