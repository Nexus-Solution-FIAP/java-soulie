package br.com.fiap.soulie.api.dto;

public record ChatContext(
        ChatMissionContext mission,
        Integer userLevel
) {
}
