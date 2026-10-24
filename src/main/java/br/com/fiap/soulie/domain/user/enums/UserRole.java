package br.com.fiap.soulie.domain.user.enums;

/**
 * Papel do usuário, usado nas decisões de autorização.
 *
 * <p>Antes não existia papel nenhum: {@code User.getAuthorities()} devolvia
 * {@code ROLE_USER} fixo, então o {@code @EnableMethodSecurity} que o projeto já
 * tinha ligado não servia para nada, e operações de catálogo como
 * {@code POST /action} ficavam abertas a qualquer usuário autenticado.
 *
 * <p>Não há rota para promover alguém a {@link #ADMIN}, de propósito — uma rota
 * assim seria o alvo óbvio de escalonamento de privilégio. O primeiro
 * administrador é promovido direto no banco; veja a migration V22.
 */
public enum UserRole {

    /** Papel padrão de quem se cadastra. */
    USER,

    /** Pode escrever no que é compartilhado por todos, como o catálogo de ações. */
    ADMIN;

    /** Nome da authority no formato que o Spring Security espera. */
    public String authority() {
        return "ROLE_" + name();
    }
}
