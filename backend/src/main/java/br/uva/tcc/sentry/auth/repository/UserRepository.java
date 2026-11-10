package br.uva.tcc.sentry.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.uva.tcc.sentry.auth.domain.Role;
import br.uva.tcc.sentry.auth.domain.User;

/** Os e-mails são gravados normalizados (trim + minúsculas); as buscas devem receber o e-mail já normalizado. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    /** Usado para impedir que o sistema fique sem nenhum administrador ativo. */
    long countByRoleAndActiveTrue(Role role);
}
