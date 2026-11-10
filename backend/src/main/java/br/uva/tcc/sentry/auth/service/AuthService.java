package br.uva.tcc.sentry.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uva.tcc.sentry.auth.DTO.ChangePasswordRequest;
import br.uva.tcc.sentry.auth.DTO.LoginRequest;
import br.uva.tcc.sentry.auth.DTO.LoginResponse;
import br.uva.tcc.sentry.auth.DTO.UserResponse;
import br.uva.tcc.sentry.auth.domain.User;
import br.uva.tcc.sentry.auth.exception.InvalidCredentialsException;
import br.uva.tcc.sentry.auth.exception.InvalidPasswordException;
import br.uva.tcc.sentry.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;

    /**
     * Hash de uma senha qualquer. Quando o e-mail não existe, comparamos contra
     * ele mesmo assim, para que "e-mail inexistente" e "senha errada" levem o
     * mesmo tempo e não dê para descobrir e-mails cadastrados pelo tempo de resposta.
     */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.dummyHash = passwordEncoder.encode("sentry-dummy-password-for-timing");
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(Emails.normalize(request.email())).orElse(null);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user != null ? user.getPasswordHash() : dummyHash);

        // mesma resposta para e-mail inexistente, senha errada e usuário desativado
        if (user == null || !passwordMatches || !user.isActive()) {
            throw new InvalidCredentialsException();
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        TokenIssuer.IssuedToken token = tokenIssuer.issue(user);

        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    /**
     * Troca da própria senha. Os tokens emitidos antes da troca deixam de valer
     * (ver {@code passwordChangedAt}), então o usuário precisa entrar de novo.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            // 400 e não 401: um 401 faria o frontend deslogar o usuário por ter errado a senha atual
            throw new InvalidPasswordException("A senha atual está incorreta.");
        }

        PasswordPolicy.validate(request.newPassword());

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("A nova senha deve ser diferente da atual.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }
}
