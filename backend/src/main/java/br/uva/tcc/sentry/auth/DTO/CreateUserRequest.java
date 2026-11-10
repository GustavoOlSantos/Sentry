package br.uva.tcc.sentry.auth.DTO;

import br.uva.tcc.sentry.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A regra de força da senha fica em {@code PasswordPolicy}. */
public record CreateUserRequest(
    @NotBlank(message = "O nome é obrigatório.") @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.") String name,
    @NotBlank(message = "O e-mail é obrigatório.") @Email(message = "E-mail inválido.") @Size(max = 180, message = "O e-mail deve ter no máximo 180 caracteres.") String email,
    @NotBlank(message = "A senha é obrigatória.") String password,
    @NotNull(message = "O perfil (role) é obrigatório.") Role role
) {
}
