package br.com.fiap.soulie.domain.avatar.enums;

import lombok.Getter;

@Getter
public enum Humor {

    WITHERED(1, "Murcho"),
    SAD(1, "Tristinho"),
    NEUTRAL(1, "Normal"),
    HAPPY(1, "Feliz"),
    RADIANT(1, "Radiante");

    private final int id;
    private final String descricao;

    Humor(int id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }
}
