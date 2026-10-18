package br.com.fiap.soulie.domain.avatar.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FlowerType {
    GIRASSOL("girassol"),
    ROSA("rosa"),
    HIBISCO("hibisco"),
    LAVANDA("lavanda"),
    MARGARIDA("margarida"),
    ORQUIDEA("orquidea");

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
        for (FlowerType flower : values()) {
            if (flower.value.equalsIgnoreCase(raw) || flower.name().equalsIgnoreCase(raw)) {
                return flower;
            }
        }
        throw new IllegalArgumentException("Flor inválida: " + raw);
    }
}
