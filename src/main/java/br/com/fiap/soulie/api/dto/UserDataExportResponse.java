package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Cópia dos dados da conta para o próprio usuário")
public record UserDataExportResponse(
        UserProfileResponse profile,
        List<ActivityResponse> activities,
        List<AchievementResponse> achievements
) {
}
