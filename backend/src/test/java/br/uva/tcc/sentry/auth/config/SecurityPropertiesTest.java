package br.uva.tcc.sentry.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

    private static final String GOOD_SECRET = "0123456789abcdef0123456789abcdef"; // 32 bytes

    @Test
    void jwtSecret_isRequired() {
        assertThrows(IllegalStateException.class, () -> new SecurityProperties.Jwt(null, Duration.ofHours(2), "sentry"));
        assertThrows(IllegalStateException.class, () -> new SecurityProperties.Jwt("", Duration.ofHours(2), "sentry"));
    }

    @Test
    void jwtSecret_shorterThan32Bytes_isRejected() {
        assertThrows(IllegalStateException.class,
                () -> new SecurityProperties.Jwt("curta-demais", Duration.ofHours(2), "sentry"));
    }

    @Test
    void jwtSecret_countsBytesNotCharacters() {
        String sixteenAccented = "é".repeat(16); // 16 caracteres, 32 bytes
        assertEquals(sixteenAccented, new SecurityProperties.Jwt(sixteenAccented, Duration.ofHours(2), "sentry").secret());
    }

    @Test
    void jwtExpiration_mustBePositive() {
        assertThrows(IllegalStateException.class, () -> new SecurityProperties.Jwt(GOOD_SECRET, Duration.ZERO, "sentry"));
        assertThrows(IllegalStateException.class, () -> new SecurityProperties.Jwt(GOOD_SECRET, Duration.ofMinutes(-5), "sentry"));
    }

    @Test
    void bootstrapAdmin_isConfiguredOnlyWithEmailAndPassword() {
        assertTrue(new SecurityProperties.BootstrapAdmin("Admin", "a@b.com", "12345678").isConfigured());
        assertFalse(new SecurityProperties.BootstrapAdmin("Admin", "a@b.com", "").isConfigured());
        assertFalse(new SecurityProperties.BootstrapAdmin("Admin", null, "12345678").isConfigured());
        assertFalse(new SecurityProperties.BootstrapAdmin("Admin", " ", "12345678").isConfigured());
    }
}
