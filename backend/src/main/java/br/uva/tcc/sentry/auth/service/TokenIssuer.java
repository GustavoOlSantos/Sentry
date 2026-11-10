package br.uva.tcc.sentry.auth.service;

import java.time.Instant;

import br.uva.tcc.sentry.auth.domain.User;

/**
 * Emite o token de acesso de um usuário. É uma interface para que as regras de
 * login possam ser testadas sem depender do Spring Security; a implementação
 * real é o {@code JwtTokenService}.
 */
public interface TokenIssuer {

    IssuedToken issue(User user);

    record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {
    }
}
