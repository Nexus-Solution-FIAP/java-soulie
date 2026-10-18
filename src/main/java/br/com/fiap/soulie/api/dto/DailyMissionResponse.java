package br.com.fiap.soulie.api.dto;

public record DailyMissionResponse(
        Long id,
        String emoji,
        String name,
        String tag,
        String desc,
        int xp,
        boolean done,
        String completedAt
) {
}
