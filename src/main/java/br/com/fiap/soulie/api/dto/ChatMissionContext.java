package br.com.fiap.soulie.api.dto;

public record ChatMissionContext(
        String name,
        String desc,
        Integer xp,
        String tag
) {
}
