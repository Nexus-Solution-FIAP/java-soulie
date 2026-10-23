package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nome e flor do Soulie. Envie só o que for mudar.")
public record UpdateSoulieRequest(
        @Schema(example = "Soulie da Sorte") String name,
        @Schema(example = "girassol", description = "girassol, rosa, hibisco, lavanda, margarida, orquidea, ipe_amarelo, guaimbe_ondulado, manaca_da_serra, maranta, pacova, pitangueira, rainha_do_abismo, trevo_roxo, mulungu, sucupira_preta ou arvore_tecnologica") String flowerType
) {
}
