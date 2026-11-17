package br.uva.tcc.sentry.report.DTO;

import java.util.List;

public record ReportSummaryResponse(
        Period period,
        String scopeDescription,
        long totalFindings,
        long detectedInPeriod,
        long carriedOver,
        long concludedInPeriod,
        long open,
        long inProgress,
        long resolved,
        long validated,
        long resolvedOrValidated,
        long acceptedRisk,
        long critical,
        long high,
        long pendingCritical,
        long pendingHigh,
        long assetsAnalyzed,
        long assetsWithFindings,
        MttrSummary mttr,
        ComplianceSummary compliance,
        List<String> limitations) {

    public record MttrSummary(Long averageSeconds, String averageLabel, long completedFindings) {
    }

    public record ComplianceSummary(Double percentage, long evaluations) {
    }
}
