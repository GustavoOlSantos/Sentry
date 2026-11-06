package br.uva.tcc.sentry.finding.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.finding.exception.InvalidFindingTransitionException;
import br.uva.tcc.sentry.shared.domain.Severity;

/**
 * A matriz de decisão do ciclo de vida, uma linha por teste. Não usa banco,
 * Spring nem Nmap: só a entidade.
 */
class FindingLifecycleTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime SCAN_STARTED = T0.plusDays(1);
    private static final LocalDateTime NOW = SCAN_STARTED.plusMinutes(5);

    private Finding finding(FindingStatus status) {
        Finding f = new Finding();
        f.setStatus(status);
        f.setSeverity(Severity.HIGH);
        f.setEvidence("evidência antiga");
        f.setDetectedAt(T0);
        f.setLastSeenAt(T0);
        return f;
    }

    private Finding observed() {
        Finding f = new Finding();
        f.setSeverity(Severity.CRITICAL);
        f.setTitle("título novo");
        f.setEvidence("evidência nova");
        return f;
    }

    // ---------------- reencontrado (onSeenAgain) ----------------

    @Test
    void openSeenAgain_staysOpenAndRefreshesDetails() {
        Finding f = finding(FindingStatus.OPEN);
        f.onSeenAgain(observed(), SCAN_STARTED, NOW);

        assertEquals(FindingStatus.OPEN, f.getStatus());
        assertEquals(NOW, f.getLastSeenAt());
        assertEquals(Severity.CRITICAL, f.getSeverity());
        assertEquals("evidência nova", f.getEvidence());
        assertEquals(T0, f.getDetectedAt());
    }

    @Test
    void inProgressSeenAgain_staysInProgress() {
        Finding f = finding(FindingStatus.IN_PROGRESS);
        f.onSeenAgain(observed(), SCAN_STARTED, NOW);
        assertEquals(FindingStatus.IN_PROGRESS, f.getStatus());
    }

    @Test
    void resolvedStillThere_failsValidation() {
        Finding f = finding(FindingStatus.RESOLVED);
        f.setResolvedAt(T0.plusHours(1));

        f.onSeenAgain(observed(), SCAN_STARTED, NOW);

        assertEquals(FindingStatus.FAILED_VALIDATION, f.getStatus());
    }

    @Test
    void resolvedSeenByScanThatStartedBeforeTheResolution_isIgnored() {
        Finding f = finding(FindingStatus.RESOLVED);
        f.setResolvedAt(SCAN_STARTED.plusMinutes(1)); // usuário resolveu com o scan já em andamento

        f.onSeenAgain(observed(), SCAN_STARTED, NOW);

        assertEquals(FindingStatus.RESOLVED, f.getStatus());
    }

    @Test
    void failedValidationSeenAgain_staysFailed() {
        Finding f = finding(FindingStatus.FAILED_VALIDATION);
        f.onSeenAgain(observed(), SCAN_STARTED, NOW);
        assertEquals(FindingStatus.FAILED_VALIDATION, f.getStatus());
    }

    @Test
    void validatedSeenAgain_isReopened() {
        Finding f = finding(FindingStatus.VALIDATED);
        f.setResolvedAt(T0.plusHours(1));
        f.setValidatedAt(T0.plusHours(2));

        f.onSeenAgain(observed(), SCAN_STARTED, NOW);

        assertEquals(FindingStatus.OPEN, f.getStatus());
        assertEquals(1, f.getReopenCount());
        assertNull(f.getValidatedAt());
        assertNull(f.getResolvedAt());
    }

    @Test
    void validatedSeenByScanThatStartedBeforeTheValidation_isNotReopened() {
        Finding f = finding(FindingStatus.VALIDATED);
        f.setValidatedAt(SCAN_STARTED.plusMinutes(1));

        f.onSeenAgain(observed(), SCAN_STARTED, NOW);

        assertEquals(FindingStatus.VALIDATED, f.getStatus());
        assertEquals(0, f.getReopenCount());
    }

    @Test
    void justifiedSeenAgain_staysJustified() {
        Finding f = finding(FindingStatus.JUSTIFIED);
        f.onSeenAgain(observed(), SCAN_STARTED, NOW);
        assertEquals(FindingStatus.JUSTIFIED, f.getStatus());
        assertEquals(NOW, f.getLastSeenAt());
    }

    // ---------------- não reencontrado (onNotSeen) ----------------

    @Test
    void openNotSeen_isValidated() {
        Finding f = finding(FindingStatus.OPEN);
        f.onNotSeen(SCAN_STARTED, NOW);

        assertEquals(FindingStatus.VALIDATED, f.getStatus());
        assertEquals(NOW, f.getValidatedAt());
        assertEquals(NOW, f.getResolvedAt());
    }

    @Test
    void inProgressNotSeen_isValidated() {
        Finding f = finding(FindingStatus.IN_PROGRESS);
        f.onNotSeen(SCAN_STARTED, NOW);
        assertEquals(FindingStatus.VALIDATED, f.getStatus());
    }

    @Test
    void resolvedNotSeen_isValidatedKeepingTheUsersResolvedAt() {
        Finding f = finding(FindingStatus.RESOLVED);
        LocalDateTime resolvedAt = T0.plusHours(1);
        f.setResolvedAt(resolvedAt);

        f.onNotSeen(SCAN_STARTED, NOW);

        assertEquals(FindingStatus.VALIDATED, f.getStatus());
        assertEquals(resolvedAt, f.getResolvedAt());
        assertEquals(NOW, f.getValidatedAt());
    }

    @Test
    void resolvedNotSeenByScanThatStartedBeforeTheResolution_isIgnored() {
        Finding f = finding(FindingStatus.RESOLVED);
        f.setResolvedAt(SCAN_STARTED.plusMinutes(1));

        f.onNotSeen(SCAN_STARTED, NOW);

        assertEquals(FindingStatus.RESOLVED, f.getStatus());
    }

    @Test
    void failedValidationNotSeen_isValidated() {
        Finding f = finding(FindingStatus.FAILED_VALIDATION);
        f.setResolvedAt(T0.plusHours(1));

        f.onNotSeen(SCAN_STARTED, NOW);

        assertEquals(FindingStatus.VALIDATED, f.getStatus());
    }

    @Test
    void staleAbsence_isIgnoredWhenAnotherScanSawTheFindingAfterThisOneStarted() {
        Finding f = finding(FindingStatus.OPEN);
        f.setLastSeenAt(SCAN_STARTED.plusMinutes(2)); // outro scan reencontrou depois do início deste

        f.onNotSeen(SCAN_STARTED, NOW);

        assertEquals(FindingStatus.OPEN, f.getStatus());
    }

    @Test
    void justifiedAndValidatedNotSeen_doNotChange() {
        Finding justified = finding(FindingStatus.JUSTIFIED);
        justified.onNotSeen(SCAN_STARTED, NOW);
        assertEquals(FindingStatus.JUSTIFIED, justified.getStatus());

        Finding validated = finding(FindingStatus.VALIDATED);
        validated.onNotSeen(SCAN_STARTED, NOW);
        assertEquals(FindingStatus.VALIDATED, validated.getStatus());
        assertNull(validated.getValidatedAt());
    }

    // ---------------- ações do usuário (changeStatusByUser) ----------------

    @Test
    void user_cannotSetToolOnlyStatuses() {
        Finding f = finding(FindingStatus.OPEN);

        assertThrows(InvalidFindingTransitionException.class,
                () -> f.changeStatusByUser(FindingStatus.VALIDATED, null, NOW));
        assertThrows(InvalidFindingTransitionException.class,
                () -> f.changeStatusByUser(FindingStatus.FAILED_VALIDATION, null, NOW));
        assertThrows(InvalidFindingTransitionException.class,
                () -> f.changeStatusByUser(null, null, NOW));
        assertEquals(FindingStatus.OPEN, f.getStatus());
    }

    @Test
    void user_cannotChangeAClosedFinding() {
        Finding f = finding(FindingStatus.VALIDATED);

        assertThrows(InvalidFindingTransitionException.class,
                () -> f.changeStatusByUser(FindingStatus.OPEN, null, NOW));
        assertEquals(FindingStatus.VALIDATED, f.getStatus());
    }

    @Test
    void justified_requiresAJustification() {
        Finding f = finding(FindingStatus.OPEN);

        assertThrows(InvalidFindingTransitionException.class,
                () -> f.changeStatusByUser(FindingStatus.JUSTIFIED, "   ", NOW));

        f.changeStatusByUser(FindingStatus.JUSTIFIED, "  risco aceito pela diretoria ", NOW);
        assertEquals(FindingStatus.JUSTIFIED, f.getStatus());
        assertEquals("risco aceito pela diretoria", f.getJustification());
    }

    @Test
    void resolved_setsResolvedAtOnlyOnTheTransition() {
        Finding f = finding(FindingStatus.IN_PROGRESS);

        f.changeStatusByUser(FindingStatus.RESOLVED, null, NOW);
        assertEquals(FindingStatus.RESOLVED, f.getStatus());
        assertEquals(NOW, f.getResolvedAt());

        f.changeStatusByUser(FindingStatus.RESOLVED, null, NOW.plusHours(3)); // reenvio: não "reinicia" o relógio
        assertEquals(NOW, f.getResolvedAt());
    }

    @Test
    void backToOpenOrInProgress_clearsResolutionData() {
        Finding f = finding(FindingStatus.FAILED_VALIDATION);
        f.setResolvedAt(T0.plusHours(1));

        f.changeStatusByUser(FindingStatus.IN_PROGRESS, null, NOW);

        assertEquals(FindingStatus.IN_PROGRESS, f.getStatus());
        assertNull(f.getResolvedAt());
        assertNotNull(f.getDetectedAt());
    }
}
