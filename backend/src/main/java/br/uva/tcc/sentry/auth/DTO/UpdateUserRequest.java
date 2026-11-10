package br.uva.tcc.sentry.auth.DTO;

import br.uva.tcc.sentry.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Substituição completa (PUT) dos dados editáveis; a senha tem endpoint próprio. */
public record UpdateUserRequest(
    @NotBlank(message = "O nome é obrigatório.") @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.") String name,
    @NotBlank(message = "O e-mail é obrigatório.") @Email(message = "E-mail inválido.") @Size(max = 180, message = "O e-mail deve ter no máximo 180 caracteres.") String email,
    @NotNull(message = "O perfil (role) é obrigatório.") Role role,
    @NotNull(message = "O campo 'active' é obrigatório.") Boolean active
) {
}
