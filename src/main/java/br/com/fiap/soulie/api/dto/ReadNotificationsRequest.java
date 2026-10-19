package br.com.fiap.soulie.api.dto;

import java.util.List;

public record ReadNotificationsRequest(List<Long> ids) {
}
