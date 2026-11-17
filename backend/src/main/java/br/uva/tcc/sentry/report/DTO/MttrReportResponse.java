package br.uva.tcc.sentry.report.DTO;

import java.util.List;

import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.ReportSeverity;

public record MttrReportResponse(
        Period period,
        String scopeDescription,
        Long averageSeconds,
        Double averageHours,
        String averageLabel,
        Long medianSeconds,
        String medianLabel,
        Long minSeconds,
        String minLabel,
        Long maxSeconds,
        String maxLabel,
        long completedFindings,
        long pendingFindings,
        List<SeverityMttr> bySeverity,
        List<TypeMttr> byType) {

    public record SeverityMttr(ReportSeverity severity, Long averageSeconds, Double averageHours,
                               String averageLabel, long completedCount) {
    }

    public record TypeMttr(FindingType type, Long averageSeconds, Double averageHours,
                           String averageLabel, long completedCount) {
    }
}
