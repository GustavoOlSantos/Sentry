package br.uva.tcc.sentry.scan.services;

import java.util.Map;

import br.uva.tcc.sentry.scan.domain.ScanStatus;

/**
 * Calcula o status "real" de um grupo de scans a partir do status dos scans dele.
 *
 * O status gravado no grupo não serve para exibição: ele vira COMPLETED assim que
 * nenhum scan está PENDING (mesmo que algum ainda esteja RUNNING ou tenha falhado).
 * Aqui o resultado é derivado dos scans:
 * <ul>
 *   <li>algum RUNNING, ou PENDING misturado com finalizados → RUNNING;</li>
 *   <li>todos PENDING → PENDING;</li>
 *   <li>todos terminaram iguais (COMPLETED, FAILED, NOT_FOUND ou CANCELLED) → esse status;</li>
 *   <li>terminaram misturados com ao menos um COMPLETED → PARTIAL;</li>
 *   <li>terminaram misturados sem nenhum COMPLETED → FAILED.</li>
 * </ul>
 */
public final class ScanGroupStatusResolver {

    private ScanGroupStatusResolver() {
    }

    public static ScanStatus resolve(ScanStatus storedStatus, Map<ScanStatus, Long> scansByStatus) {
        long total = scansByStatus.values().stream().mapToLong(Long::longValue).sum();

        if (total == 0) {
            return storedStatus; // grupo sem scans: só resta o que foi gravado
        }

        long pending = count(scansByStatus, ScanStatus.PENDING);
        long running = count(scansByStatus, ScanStatus.RUNNING);
        long completed = count(scansByStatus, ScanStatus.COMPLETED);

        if (running > 0) {
            return ScanStatus.RUNNING;
        }
        if (pending > 0) {
            return pending == total ? ScanStatus.PENDING : ScanStatus.RUNNING;
        }

        // todos os scans chegaram a um estado final
        for (ScanStatus terminal : new ScanStatus[] {
                ScanStatus.COMPLETED, ScanStatus.FAILED, ScanStatus.NOT_FOUND, ScanStatus.CANCELLED }) {
            if (count(scansByStatus, terminal) == total) {
                return terminal;
            }
        }

        return completed > 0 ? ScanStatus.PARTIAL : ScanStatus.FAILED;
    }

    private static long count(Map<ScanStatus, Long> scansByStatus, ScanStatus status) {
        return scansByStatus.getOrDefault(status, 0L);
    }
}
