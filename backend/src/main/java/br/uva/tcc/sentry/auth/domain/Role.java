package br.uva.tcc.sentry.auth.domain;

/**
 * Perfis de acesso.
 * <ul>
 *   <li>{@link #ADM}: tudo, inclusive gestão de usuários, exclusão de assets e políticas;</li>
 *   <li>{@link #DEV}: leitura + tratar findings (status, responsável), executar scans e editar assets;</li>
 *   <li>{@link #USER}: somente leitura.</li>
 * </ul>
 * A matriz completa de rotas está em {@code SecurityConfig}.
 */
public enum Role {
    ADM,
    DEV,
    USER;

    /** Nome da authority no Spring Security (o {@code hasRole("ADM")} espera o prefixo ROLE_). */
    public String authority() {
        return "ROLE_" + name();
    }
}
