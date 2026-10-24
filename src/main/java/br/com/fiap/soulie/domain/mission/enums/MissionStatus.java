package br.com.fiap.soulie.domain.mission.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MissionStatus {

    ACTIVE(1, "Ativa"),
    IN_PROGRESS(2, "Em Andamento"),
    COMPLETED(3, "Completa"),
    EXPIRED(4, "Expirada");

    private final int code;
    private final String name;

}
