package br.com.fiap.soulie.infra.messaging;

/**
 * Evento interno disparado quando uma evidencia entra para validacao.
 *
 * <p>Existe para separar "registrei a evidencia" de "publiquei na fila". A
 * publicacao escuta este evento em AFTER_COMMIT — ver ValidationPublisher.
 */
public record ValidationRequestedEvent(Long validationId) {
}
