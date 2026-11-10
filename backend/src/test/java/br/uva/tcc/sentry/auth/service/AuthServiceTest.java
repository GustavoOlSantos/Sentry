package br.uva.tcc.sentry.auth.service;

import static br.uva.tcc.sentry.auth.service.AuthTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.auth.DTO.ChangePasswordRequest;
import br.uva.tcc.sentry.auth.DTO.LoginRequest;
import br.uva.tcc.sentry.auth.DTO.LoginResponse;
import br.uva.tcc.sentry.auth.DTO.UserResponse;
import br.uva.tcc.sentry.auth.domain.Role;
import br.uva.tcc.sentry.auth.domain.User;
import br.uva.tcc.sentry.auth.exception.InvalidCredentialsException;
import br.uva.tcc.sentry.auth.exception.InvalidPasswordException;

class AuthServiceTest {

    private final List<User> store = new ArrayList<>();
    private AuthTestSupport.FakePasswordEncoder encoder;
    private AuthService authService;
    private User ana;

    @BeforeEach
    void setUp() {
        encoder = new AuthTestSupport.FakePasswordEncoder();
        TokenIssuer issuer = u -> new TokenIssuer.IssuedToken("token-do-usuario-" + u.getId(), Instant.now().plusSeconds(7200), 7200);
        authService = new AuthService(AuthTestSupport.inMemoryRepository(store), encoder, issuer);

        ana = user(1L, "ana@sentry.com", encoder.encode("senha-da-ana"), Role.DEV, true);
        store.add(ana);
    }

    // ---------------- login ----------------

    @Test
    void login_withValidCredentials_returnsTokenAndUser() {
        LoginResponse response = authService.login(new LoginRequest("ana@sentry.com", "senha-da-ana"));

        assertEquals("token-do-usuario-1", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(7200, response.expiresIn());
        assertEquals("ana@sentry.com", response.user().email());
        assertEquals(Role.DEV, response.user().role());
    }

    @Test
    void login_ignoresCaseAndSurroundingSpacesInTheEmail() {
        LoginResponse response = authService.login(new LoginRequest("  ANA@Sentry.com ", "senha-da-ana"));
        assertEquals(1L, response.user().id());
    }

    @Test
    void login_updatesLastLoginAt() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        authService.login(new LoginRequest("ana@sentry.com", "senha-da-ana"));
        assertTrue(ana.getLastLoginAt().isAfter(before));
    }

    @Test
    void login_withWrongPassword_isRejected() {
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ana@sentry.com", "errada")));
    }

    @Test
    void login_withUnknownEmail_isRejectedWithTheSameErrorAndStillComparesAHash() {
        int before = encoder.matchesCalls;

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ninguem@sentry.com", "qualquer")));

        assertEquals("E-mail ou senha inválidos.", ex.getMessage());
        assertEquals(before + 1, encoder.matchesCalls); // mesmo custo de uma senha errada: não revela quais e-mails existem
    }

    @Test
    void login_withInactiveUser_isRejectedEvenWithTheRightPassword() {
        ana.setActive(false);
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ana@sentry.com", "senha-da-ana")));
    }

    @Test
    void responses_neverCarryThePasswordHash() {
        for (Class<?> type : List.of(UserResponse.class, LoginResponse.class)) {
            boolean leaks = Arrays.stream(type.getRecordComponents())
                    .map(RecordComponent::getName)
                    .anyMatch(name -> name.toLowerCase().contains("password") || name.toLowerCase().contains("hash"));
            assertFalse(leaks, type.getSimpleName() + " não pode expor senha/hash");
        }
        assertFalse(ana.toString().contains(ana.getPasswordHash()));
    }

    // ---------------- troca de senha ----------------

    @Test
    void changePassword_updatesTheHashAndInvalidatesOlderTokens() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        authService.changePassword(1L, new ChangePasswordRequest("senha-da-ana", "nova-senha-123"));

        assertEquals("enc(nova-senha-123)", ana.getPasswordHash());
        assertTrue(ana.getPasswordChangedAt().isAfter(before));
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ana@sentry.com", "senha-da-ana")));
        assertNotNull(authService.login(new LoginRequest("ana@sentry.com", "nova-senha-123")));
    }

    @Test
    void changePassword_withWrongCurrentPassword_isRejected() {
        assertThrows(InvalidPasswordException.class,
                () -> authService.changePassword(1L, new ChangePasswordRequest("errada", "nova-senha-123")));
        assertEquals("enc(senha-da-ana)", ana.getPasswordHash());
    }

    @Test
    void changePassword_withTheSamePassword_isRejected() {
        assertThrows(InvalidPasswordException.class,
                () -> authService.changePassword(1L, new ChangePasswordRequest("senha-da-ana", "senha-da-ana")));
    }

    @Test
    void changePassword_withWeakPassword_isRejected() {
        assertThrows(InvalidPasswordException.class,
                () -> authService.changePassword(1L, new ChangePasswordRequest("senha-da-ana", "curta")));
    }
}
