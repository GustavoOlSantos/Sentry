package br.uva.tcc.sentry.report.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.EvolutionGroupBy;
import br.uva.tcc.sentry.report.ReportSeverity;
import br.uva.tcc.sentry.scan.domain.ScanStatus;

public record RiskEvolutionResponse(
        Period period,
        EvolutionGroupBy groupBy,
        String scopeDescription,
        List<RiskEvolutionPoint> points,
        List<ScanEvolutionPoint> scans,
        List<String> limitations) {

    public record RiskEvolutionPoint(
            LocalDate date,
            long detected,
            long resolved,
            long pending,
            long acceptedRisk,
            Map<ReportSeverity, Long> bySeverity) {
    }

    public record ScanEvolutionPoint(
            UUID scanId,
            UUID scanGroupId,
            String target,
            ScanStatus status,
            LocalDateTime executedAt,
            long observed,
            long newFindings,
            long carriedOver,
            long resolvedByScan,
            long pending,
            Long changeFromPrevious,
            Map<ReportSeverity, Long> bySeverity,
            Map<FindingType, Long> byType) {
    }
}
