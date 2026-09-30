package br.com.fiap.soulie.domain.avatar.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Humor {

    WITHERED(1, "Murcho"),
    SAD(1, "Tristinho"),
    NEUTRAL(1, "Normal"),
    HAPPY(1, "Feliz"),
    RADIANT(1, "Radiante");

    private final int id;
    private final String descricao;
}
