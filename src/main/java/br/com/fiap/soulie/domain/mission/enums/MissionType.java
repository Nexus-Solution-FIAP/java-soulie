package br.com.fiap.soulie.domain.mission.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MissionType {

    DAILY(1, "Diária"),
    WEEKLY(2, "Semanal"),
    MONTHLY(3, "Mensal"),
    SEMIANNUAL(4, "Semestral"),
    ANUAL(5, "Anual");

    private final int code;
    private final String name;
}
