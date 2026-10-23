package br.com.fiap.soulie.domain.history_points.dto;

import br.com.fiap.soulie.domain.history_points.HistoryPoints;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record HistoryPointsResponse(

        Long id,

        Long actionId,

        String actionName,

        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate date,

        int pointsEarned
) {
    public HistoryPointsResponse(HistoryPoints historyPoints){
        this(
                historyPoints.getId(),
                historyPoints.getAction().getId(),
                historyPoints.getAction().getName(),
                historyPoints.getDate(),
                historyPoints.getPointsEarned()
        );
    }
}
