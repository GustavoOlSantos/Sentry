package br.uva.tcc.sentry.auth.DTO;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
    @NotBlank(message = "A senha atual é obrigatória.") String currentPassword,
    @NotBlank(message = "A nova senha é obrigatória.") String newPassword
) {
}
