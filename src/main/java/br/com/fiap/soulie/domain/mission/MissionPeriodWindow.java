package br.com.fiap.soulie.domain.mission;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public final class MissionPeriodWindow {

    private MissionPeriodWindow() {
    }

    public static LocalDate start(MissionPeriod period, LocalDate today) {
        MissionPeriod resolved = period == null ? MissionPeriod.DAILY : period;
        return switch (resolved) {
            case DAILY -> today;
            case WEEKLY -> today.with(DayOfWeek.MONDAY);
            case MONTHLY -> today.withDayOfMonth(1);
            case YEARLY -> today.withDayOfYear(1);
        };
    }

    public static LocalDate end(MissionPeriod period, LocalDate today) {
        MissionPeriod resolved = period == null ? MissionPeriod.DAILY : period;
        return switch (resolved) {
            case DAILY -> today;
            case WEEKLY -> today.with(DayOfWeek.MONDAY).plusDays(6);
            case MONTHLY -> today.with(TemporalAdjusters.lastDayOfMonth());
            case YEARLY -> today.with(TemporalAdjusters.lastDayOfYear());
        };
    }

    public static boolean contains(MissionPeriod period, LocalDate today, LocalDate completedOn) {
        if (completedOn == null) {
            return false;
        }
        LocalDate from = start(period, today);
        LocalDate to = end(period, today);
        return !completedOn.isBefore(from) && !completedOn.isAfter(to);
    }

    public static String alreadyDoneMessage(MissionPeriod period) {
        MissionPeriod resolved = period == null ? MissionPeriod.DAILY : period;
        return switch (resolved) {
            case DAILY -> "Missão já concluída hoje";
            case WEEKLY -> "Missão já concluída nesta semana";
            case MONTHLY -> "Missão já concluída neste mês";
            case YEARLY -> "Missão já concluída neste ano";
        };
    }
}
