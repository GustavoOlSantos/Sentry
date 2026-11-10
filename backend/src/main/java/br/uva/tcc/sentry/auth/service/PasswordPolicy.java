package br.uva.tcc.sentry.auth.service;

import java.nio.charset.StandardCharsets;

import br.uva.tcc.sentry.auth.exception.InvalidPasswordException;

/**
 * Regra de senha aceita pelo sistema. O limite de 72 BYTES existe porque o
 * BCrypt só considera os 72 primeiros bytes (e o Spring Security recusa mais que isso);
 * como caracteres acentuados ocupam mais de um byte, o limite é checado em bytes.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new InvalidPasswordException("A senha é obrigatória.");
        }
        if (password.length() < MIN_LENGTH) {
            throw new InvalidPasswordException("A senha deve ter pelo menos " + MIN_LENGTH + " caracteres.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new InvalidPasswordException("A senha é longa demais (máximo de " + MAX_BYTES + " bytes).");
        }
    }
}
