package br.com.fiap.soulie.domain.avatar.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FlowerType {
    GIRASSOL("girassol"),
    ROSA("rosa"),
    HIBISCO("hibisco"),
    LAVANDA("lavanda"),
    MARGARIDA("margarida"),
    ORQUIDEA("orquidea"),
    IPE_AMARELO("ipe_amarelo"),
    GUAIMBE_ONDULADO("guaimbe_ondulado"),
    MANACA_DA_SERRA("manaca_da_serra"),
    MARANTA("maranta"),
    PACOVA("pacova"),
    PITANGUEIRA("pitangueira"),
    RAINHA_DO_ABISMO("rainha_do_abismo"),
    TREVO_ROXO("trevo_roxo"),
    MULUNGU("mulungu"),
    SUCUPIRA_PRETA("sucupira_preta");

    private final String value;

    FlowerType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FlowerType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_');
        for (FlowerType flower : values()) {
            if (flower.value.equalsIgnoreCase(normalized) || flower.name().equalsIgnoreCase(normalized)) {
                return flower;
            }
        }
        throw new IllegalArgumentException("Planta inválida: " + raw);
    }
}
