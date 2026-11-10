package br.uva.tcc.sentry.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uva.tcc.sentry.auth.DTO.CreateUserRequest;
import br.uva.tcc.sentry.auth.DTO.ResetPasswordRequest;
import br.uva.tcc.sentry.auth.DTO.UpdateUserRequest;
import br.uva.tcc.sentry.auth.DTO.UserResponse;
import br.uva.tcc.sentry.auth.domain.Role;
import br.uva.tcc.sentry.auth.domain.User;
import br.uva.tcc.sentry.auth.exception.EmailAlreadyInUseException;
import br.uva.tcc.sentry.auth.exception.LastAdminException;
import br.uva.tcc.sentry.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * CRUD de usuários (uso de administradores). Só devolve {@link UserResponse};
 * a entidade, com o hash da senha, não sai daqui.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(getUser(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = Emails.normalize(request.email());
        PasswordPolicy.validate(request.password());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        user.setPasswordChangedAt(LocalDateTime.now());

        return UserResponse.from(save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getUser(id);
        String email = Emails.normalize(request.email());

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new EmailAlreadyInUseException(email);
        }

        boolean losesAdminAccess = isActiveAdmin(user)
                && (request.role() != Role.ADM || !request.active());
        if (losesAdminAccess) {
            assertNotLastAdmin("rebaixar ou desativar");
        }

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setActive(request.active());

        return UserResponse.from(save(user));
    }

    /** Definição de senha por um administrador. Invalida os tokens já emitidos para o usuário. */
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        PasswordPolicy.validate(request.newPassword());

        User user = getUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = getUser(id);

        if (isActiveAdmin(user)) {
            assertNotLastAdmin("excluir");
        }

        userRepository.delete(user);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    private boolean isActiveAdmin(User user) {
        return user.getRole() == Role.ADM && user.isActive();
    }

    private void assertNotLastAdmin(String action) {
        if (userRepository.countByRoleAndActiveTrue(Role.ADM) <= 1) {
            throw new LastAdminException(action);
        }
    }

    /** Duas requisições simultâneas com o mesmo e-mail: a constraint única do banco decide. */
    private User save(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyInUseException(user.getEmail());
        }
    }
}
