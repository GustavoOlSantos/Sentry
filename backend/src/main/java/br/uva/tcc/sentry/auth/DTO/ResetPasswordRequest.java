package br.uva.tcc.sentry.auth.DTO;

import jakarta.validation.constraints.NotBlank;

/** Definição de uma nova senha por um administrador (não exige a senha atual). */
public record ResetPasswordRequest(
    @NotBlank(message = "A nova senha é obrigatória.") String newPassword
) {
}
