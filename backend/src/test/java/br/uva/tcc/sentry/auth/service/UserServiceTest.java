package br.uva.tcc.sentry.auth.service;

import static br.uva.tcc.sentry.auth.service.AuthTestSupport.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.auth.DTO.CreateUserRequest;
import br.uva.tcc.sentry.auth.DTO.ResetPasswordRequest;
import br.uva.tcc.sentry.auth.DTO.UpdateUserRequest;
import br.uva.tcc.sentry.auth.DTO.UserResponse;
import br.uva.tcc.sentry.auth.domain.Role;
import br.uva.tcc.sentry.auth.domain.User;
import br.uva.tcc.sentry.auth.exception.EmailAlreadyInUseException;
import br.uva.tcc.sentry.auth.exception.InvalidPasswordException;
import br.uva.tcc.sentry.auth.exception.LastAdminException;
import jakarta.persistence.EntityNotFoundException;

class UserServiceTest {

    private final List<User> store = new ArrayList<>();
    private UserService userService;
    private User admin;

    @BeforeEach
    void setUp() {
        userService = new UserService(AuthTestSupport.inMemoryRepository(store), new AuthTestSupport.FakePasswordEncoder());
        admin = user(1L, "admin@sentry.com", "enc(admin-pass-1)", Role.ADM, true);
        store.add(admin);
    }

    // ---------------- criação ----------------

    @Test
    void create_normalizesEmail_hashesPassword_andActivatesTheUser() {
        UserResponse created = userService.create(
                new CreateUserRequest("  Bia Souza ", "  BIA@Sentry.com ", "senha-da-bia", Role.USER));

        assertNotNull(created.id());
        assertEquals("Bia Souza", created.name());
        assertEquals("bia@sentry.com", created.email());
        assertEquals(Role.USER, created.role());
        assertTrue(created.active());

        User stored = AuthTestSupport.find(store, "bia@sentry.com").orElseThrow();
        assertEquals("enc(senha-da-bia)", stored.getPasswordHash()); // gravou o hash, não a senha
        assertNotNull(stored.getPasswordChangedAt());
    }

    @Test
    void create_withExistingEmail_isRejected_ignoringCase() {
        assertThrows(EmailAlreadyInUseException.class,
                () -> userService.create(new CreateUserRequest("Outro", "ADMIN@sentry.com", "12345678", Role.USER)));
        assertEquals(1, store.size());
    }

    @Test
    void create_withWeakPassword_isRejected() {
        assertThrows(InvalidPasswordException.class,
                () -> userService.create(new CreateUserRequest("Bia", "bia@sentry.com", "curta", Role.USER)));
        assertEquals(1, store.size());
    }

    @Test
    void create_withPasswordLongerThan72Bytes_isRejected() {
        String longPassword = "é".repeat(40); // 40 caracteres, 80 bytes em UTF-8
        assertThrows(InvalidPasswordException.class,
                () -> userService.create(new CreateUserRequest("Bia", "bia@sentry.com", longPassword, Role.USER)));
    }

    // ---------------- leitura ----------------

    @Test
    void findAll_and_findById() {
        userService.create(new CreateUserRequest("Bia", "bia@sentry.com", "senha-da-bia", Role.DEV));

        assertEquals(2, userService.findAll().size());
        assertEquals("admin@sentry.com", userService.findById(1L).email());
    }

    @Test
    void findById_unknown_throwsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> userService.findById(99L));
    }

    // ---------------- atualização ----------------

    @Test
    void update_changesTheEditableFields() {
        User bia = user(2L, "bia@sentry.com", "enc(x)", Role.USER, true);
        store.add(bia);

        UserResponse updated = userService.update(2L, new UpdateUserRequest("Beatriz", "Beatriz@Sentry.com", Role.DEV, true));

        assertEquals("Beatriz", updated.name());
        assertEquals("beatriz@sentry.com", updated.email());
        assertEquals(Role.DEV, updated.role());
    }

    @Test
    void update_keepingTheSameEmail_isAllowed() {
        UserResponse updated = userService.update(1L, new UpdateUserRequest("Admin", "admin@sentry.com", Role.ADM, true));
        assertEquals("admin@sentry.com", updated.email());
    }

    @Test
    void update_toAnotherUsersEmail_isRejected() {
        store.add(user(2L, "bia@sentry.com", "enc(x)", Role.USER, true));

        assertThrows(EmailAlreadyInUseException.class,
                () -> userService.update(2L, new UpdateUserRequest("Bia", "admin@sentry.com", Role.USER, true)));
    }

    // ---------------- o sistema nunca fica sem administrador ----------------

    @Test
    void lastAdmin_cannotBeDeleted() {
        assertThrows(LastAdminException.class, () -> userService.delete(1L));
        assertEquals(1, store.size());
    }

    @Test
    void lastAdmin_cannotBeDemoted() {
        assertThrows(LastAdminException.class,
                () -> userService.update(1L, new UpdateUserRequest("Admin", "admin@sentry.com", Role.DEV, true)));
        assertEquals(Role.ADM, admin.getRole());
    }

    @Test
    void lastAdmin_cannotBeDeactivated() {
        assertThrows(LastAdminException.class,
                () -> userService.update(1L, new UpdateUserRequest("Admin", "admin@sentry.com", Role.ADM, false)));
        assertTrue(admin.isActive());
    }

    @Test
    void anAdmin_canBeDemotedOrDeleted_whenAnotherActiveAdminExists() {
        store.add(user(2L, "outro@sentry.com", "enc(x)", Role.ADM, true));

        userService.update(1L, new UpdateUserRequest("Admin", "admin@sentry.com", Role.DEV, true));
        assertEquals(Role.DEV, admin.getRole());

        assertThrows(LastAdminException.class, () -> userService.delete(2L)); // agora o outro é o único
    }

    @Test
    void inactiveAdmins_doNotCountAsActiveAdmins() {
        store.add(user(2L, "inativo@sentry.com", "enc(x)", Role.ADM, false));

        assertThrows(LastAdminException.class, () -> userService.delete(1L));
        userService.delete(2L); // excluir um admin já inativo não afeta ninguém
        assertEquals(1, store.size());
    }

    @Test
    void nonAdmins_canBeDeleted() {
        store.add(user(2L, "bia@sentry.com", "enc(x)", Role.USER, true));
        userService.delete(2L);
        assertFalse(AuthTestSupport.find(store, "bia@sentry.com").isPresent());
    }

    // ---------------- reset de senha por administrador ----------------

    @Test
    void resetPassword_setsANewHash_andInvalidatesOlderTokens() {
        admin.setPasswordChangedAt(null);

        userService.resetPassword(1L, new ResetPasswordRequest("nova-senha-123"));

        assertEquals("enc(nova-senha-123)", admin.getPasswordHash());
        assertNotNull(admin.getPasswordChangedAt());
    }

    @Test
    void resetPassword_withWeakPassword_isRejected() {
        assertThrows(InvalidPasswordException.class, () -> userService.resetPassword(1L, new ResetPasswordRequest("123")));
        assertEquals("enc(admin-pass-1)", admin.getPasswordHash());
    }
}
