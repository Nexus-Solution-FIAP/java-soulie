package br.com.fiap.soulie.domain.action.dto;

import br.com.fiap.soulie.domain.action.Action;

public record ActionResponse(

        Long id,
        String name,
        String description,
        int points
) {
    public ActionResponse(Action action){
        this(
                action.getId(),
                action.getName(),
                action.getDescription(),
                action.getPoints()
        );
    }
}
