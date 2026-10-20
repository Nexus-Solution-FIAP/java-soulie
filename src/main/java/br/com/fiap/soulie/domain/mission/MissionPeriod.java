package br.com.fiap.soulie.domain.mission;

import java.util.Locale;

public enum MissionPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MissionPeriod from(String value) {
        if (value == null || value.isBlank()) {
            return DAILY;
        }
        try {
            return MissionPeriod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DAILY;
        }
    }
}
