package br.uva.tcc.sentry.auth.DTO;

import java.time.LocalDateTime;

import br.uva.tcc.sentry.auth.domain.Role;
import br.uva.tcc.sentry.auth.domain.User;

/** Visão pública de um usuário: propositalmente sem o hash da senha. */
public record UserResponse(
    Long id,
    String name,
    String email,
    Role role,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt(),
            user.getLastLoginAt()
        );
    }
}
