package br.com.fiap.soulie.domain.action.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ActionRequest(

    @NotBlank
    String name,

    @NotBlank
    String description,

    @Positive
    int points


) {
}
