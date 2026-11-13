package br.uva.tcc.sentry.scan.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.uva.tcc.sentry.scan.domain.ScanStatus;

/**
 * Resumo de um grupo de scans (uma chamada de /scan/exec).
 *
 * @param status         status derivado dos scans do grupo (ver ScanGroupStatusResolver)
 * @param durationSeconds null enquanto o grupo não terminou
 * @param assetsCount    ativos distintos encontrados pelos scans do grupo
 * @param findingsCount  findings detectados pela primeira vez por scans do grupo
 */
public record ScanGroupSummaryResponse(
    UUID id,
    ScanStatus status,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Long durationSeconds,
    String errorMessage,
    int totalScans,
    Map<ScanStatus, Long> scansByStatus,
    List<String> targets,
    long assetsCount,
    long findingsCount
) {
}
