package br.com.fiap.soulie.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class TimeLabels {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    private TimeLabels() {
    }

    public static String memberSince(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "Membro desde agora";
        }
        String month = createdAt.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        String capitalized = month.substring(0, 1).toUpperCase(PT_BR) + month.substring(1);
        return "Membro desde " + capitalized + " " + createdAt.getYear();
    }

    public static String activityDate(LocalDateTime when) {
        LocalDate day = when.toLocalDate();
        LocalDate today = LocalDate.now();
        String hour = when.format(HOUR);
        if (day.equals(today)) {
            return "Hoje, " + hour;
        }
        if (day.equals(today.minusDays(1))) {
            return "Ontem, " + hour;
        }
        long days = ChronoUnit.DAYS.between(day, today);
        return "Há " + days + " dias";
    }

    public static String feedTime(LocalDateTime when) {
        long minutes = ChronoUnit.MINUTES.between(when, LocalDateTime.now());
        if (minutes < 1) {
            return "Agora mesmo";
        }
        if (minutes < 60) {
            return "há " + minutes + " min";
        }
        long hours = ChronoUnit.HOURS.between(when, LocalDateTime.now());
        if (hours < 24) {
            return "há " + hours + " h";
        }
        long days = ChronoUnit.DAYS.between(when.toLocalDate(), LocalDate.now());
        return "há " + days + " d";
    }

    public static String unlockedDate(LocalDateTime when) {
        if (when == null) {
            return "Desbloqueado";
        }
        LocalDate day = when.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) {
            return "Desbloqueado hoje!";
        }
        if (day.equals(today.minusDays(1))) {
            return "Desbloqueado ontem";
        }
        long days = ChronoUnit.DAYS.between(day, today);
        return "Desbloqueado há " + days + (days == 1 ? " dia" : " dias");
    }
}
