package br.com.fiap.soulie.infra.messaging;

/**
 * O que trafega na fila: so o identificador da validacao.
 *
 * <p>Nao vai nada de conteudo aqui — nem a imagem, nem a URL assinada, nem dado
 * do usuario. Dois motivos: a URL assinada expira e a mensagem pode ficar na
 * fila mais tempo que o prazo dela, e o corpo de uma mensagem fica legivel para
 * quem tiver acesso ao broker. O worker busca o estado atual no banco.
 */
public record ValidationMessage(Long validationId) {
}
