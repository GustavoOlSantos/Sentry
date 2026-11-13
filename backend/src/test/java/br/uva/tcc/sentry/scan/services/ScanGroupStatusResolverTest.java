package br.uva.tcc.sentry.scan.services;

import static br.uva.tcc.sentry.scan.domain.ScanStatus.CANCELLED;
import static br.uva.tcc.sentry.scan.domain.ScanStatus.COMPLETED;
import static br.uva.tcc.sentry.scan.domain.ScanStatus.FAILED;
import static br.uva.tcc.sentry.scan.domain.ScanStatus.NOT_FOUND;
import static br.uva.tcc.sentry.scan.domain.ScanStatus.PARTIAL;
import static br.uva.tcc.sentry.scan.domain.ScanStatus.PENDING;
import static br.uva.tcc.sentry.scan.domain.ScanStatus.RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.scan.domain.ScanStatus;

class ScanGroupStatusResolverTest {

    private static Map<ScanStatus, Long> counts(Object... pairs) {
        Map<ScanStatus, Long> map = new EnumMap<>(ScanStatus.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ScanStatus) pairs[i], ((Number) pairs[i + 1]).longValue());
        }
        return map;
    }

    private static ScanStatus resolve(ScanStatus stored, Object... pairs) {
        return ScanGroupStatusResolver.resolve(stored, counts(pairs));
    }

    @Test
    void groupWithoutScans_keepsTheStoredStatus() {
        assertEquals(PENDING, resolve(PENDING));
        assertEquals(COMPLETED, resolve(COMPLETED));
    }

    @Test
    void anyRunningScan_meansRunning_evenIfTheStoredStatusSaysCompleted() {
        assertEquals(RUNNING, resolve(COMPLETED, RUNNING, 1, COMPLETED, 2)); // bug do verificaGrupo: ignora RUNNING
    }

    @Test
    void allPending_isPending() {
        assertEquals(PENDING, resolve(PENDING, PENDING, 3));
    }

    @Test
    void pendingMixedWithFinishedScans_isStillRunning() {
        assertEquals(RUNNING, resolve(PENDING, PENDING, 1, COMPLETED, 1));
    }

    @Test
    void allFinishedTheSameWay_takesThatStatus() {
        assertEquals(COMPLETED, resolve(COMPLETED, COMPLETED, 3));
        assertEquals(FAILED, resolve(COMPLETED, FAILED, 2));
        assertEquals(NOT_FOUND, resolve(COMPLETED, NOT_FOUND, 1));
        assertEquals(CANCELLED, resolve(COMPLETED, CANCELLED, 2));
    }

    @Test
    void finishedMixedWithAtLeastOneCompleted_isPartial() {
        assertEquals(PARTIAL, resolve(COMPLETED, COMPLETED, 2, FAILED, 1));
        assertEquals(PARTIAL, resolve(COMPLETED, COMPLETED, 1, NOT_FOUND, 1));
        assertEquals(PARTIAL, resolve(COMPLETED, COMPLETED, 1, CANCELLED, 1, FAILED, 1));
    }

    @Test
    void finishedMixedWithoutAnyCompleted_isFailed() {
        assertEquals(FAILED, resolve(COMPLETED, FAILED, 1, NOT_FOUND, 1));
    }
}
