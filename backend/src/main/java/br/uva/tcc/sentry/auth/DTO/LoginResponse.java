package br.uva.tcc.sentry.auth.DTO;

/**
 * @param expiresIn validade do token, em segundos, a partir da emissão
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserResponse user
) {
}
