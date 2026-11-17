package br.uva.tcc.sentry.report.DTO;

import java.time.LocalDateTime;

public record FullReport(
        String reportId,
        LocalDateTime generatedAt,
        Period period,
        String scopeDescription,
        ReportSummaryResponse summary,
        RiskEvolutionResponse evolution,
        FindingsDistributionResponse distribution,
        MttrReportResponse mttr,
        ComplianceReportResponse compliance,
        RelevantFindingsResponse relevantFindings) {
}
