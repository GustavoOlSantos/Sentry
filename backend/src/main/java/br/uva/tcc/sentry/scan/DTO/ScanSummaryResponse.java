package br.uva.tcc.sentry.scan.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import br.uva.tcc.sentry.scan.domain.ScanStatus;

/** Linha de listagem de um scan individual (alvo + resultado em números). */
public record ScanSummaryResponse(
    UUID id,
    UUID groupId,
    String target,
    ScanStatus status,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Long durationSeconds,
    String errorMessage,
    long assetsCount,
    long findingsCount,
    long warningsCount
) {
}
