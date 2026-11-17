package br.uva.tcc.sentry.report.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.ReportSeverity;

public record RelevantFindingsResponse(
        Period period,
        int limit,
        long totalInScope,
        List<RelevantFinding> findings) {

    public record RelevantFinding(
            UUID id,
            String title,
            String code,
            ReportSeverity severity,
            FindingType type,
            FindingStatus status,
            String assetIp,
            String assetHostname,
            Integer port,
            String protocol,
            LocalDateTime detectedAt) {
    }
}
