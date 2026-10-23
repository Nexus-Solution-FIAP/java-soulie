package br.com.fiap.soulie.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado do envio de uma foto de missão.
 *
 * <p><b>Nenhum campo foi removido ou renomeado</b> — o app do parceiro já
 * consome este contrato. Entraram apenas {@code validationId} e {@code status}.
 * Um cliente antigo continua desserializando normalmente e lê
 * {@code xpGained: 0} enquanto a análise não terminou.
 *
 * <p>O que mudou de verdade é <i>quando</i> os valores ficam preenchidos. Antes
 * o XP vinha na hora, sem a foto ser olhada, e {@code aiDetections} vinha de uma
 * tabela de frases fixas. Agora o envio responde {@code 202} com
 * {@code status: "PENDING"}, e os campos de recompensa só se preenchem depois
 * que a IA aprova — consulte em {@code GET /v1/missions/validations/{id}} ou
 * espere o push.
 */
public record MissionValidationResponse(
        @Schema(example = "true", description = "Verdadeiro só quando a IA aprovou a foto.")
        boolean success,

        @Schema(example = "40", description = "XP creditado. Zero enquanto a validação não for aprovada.")
        int xpGained,

        @Schema(example = "2") Long missionId,

        @Schema(example = "[\"Bicicleta reconhecida no trajeto\"]",
                description = "O que a IA reconheceu na imagem. Vazio enquanto PENDING.")
        List<String> aiDetections,

        @Schema(example = "0.4") double co2SavedKg,

        BadgeUnlockedResponse badgeUnlocked,

        SuggestedMissionResponse nextSuggestedMission,

        @Schema(example = "42", description = "Identificador da validação. Use em GET /v1/missions/validations/{id}.")
        Long validationId,

        @Schema(example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED", "FAILED"},
                description = "PENDING: em análise. APPROVED: XP creditado. REJECTED: a foto não comprova. FAILED: não foi possível analisar, será reprocessada.")
        String status
) {

    /** Resposta do envio: aceito para análise, nada creditado ainda. */
    public static MissionValidationResponse pendente(Long validationId, Long missionId) {
        return new MissionValidationResponse(
                false, 0, missionId, List.of(), 0, null, null, validationId, "PENDING");
    }
}
