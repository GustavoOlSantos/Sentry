package br.uva.tcc.sentry.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.auth.domain.Role;
import br.uva.tcc.sentry.auth.domain.User;
import br.uva.tcc.sentry.auth.repository.UserRepository;
import br.uva.tcc.sentry.auth.service.UserService;

class AdminBootstrapRunnerTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final List<User> store = new ArrayList<>();
    private final UserRepository repository = BootstrapTestSupport.repository(store);
    private final UserService userService = new UserService(repository, BootstrapTestSupport.encoder());

    private AdminBootstrapRunner runner(String email, String password) {
        var properties = new SecurityProperties(
                new SecurityProperties.Jwt(SECRET, Duration.ofHours(2), "sentry"),
                new SecurityProperties.Cors(List.of("http://localhost:5173"), 3600),
                true,
                new SecurityProperties.BootstrapAdmin("Administrador", email, password));
        return new AdminBootstrapRunner(repository, userService, properties);
    }

    @Test
    void emptyDatabase_createsTheConfiguredAdmin() {
        runner("Admin@Sentry.com", "admin-pass-123").run(null);

        assertEquals(1, store.size());
        assertEquals("admin@sentry.com", store.get(0).getEmail());
        assertEquals(Role.ADM, store.get(0).getRole());
        assertTrue(store.get(0).isActive());
    }

    @Test
    void existingUsers_nothingIsCreated() {
        runner("admin@sentry.com", "admin-pass-123").run(null);
        runner("outro@sentry.com", "admin-pass-123").run(null);

        assertEquals(1, store.size());
    }

    @Test
    void notConfigured_nothingIsCreated() {
        runner("", "").run(null);
        assertEquals(0, store.size());
    }
}
