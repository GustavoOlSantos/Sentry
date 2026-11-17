package br.uva.tcc.sentry.report.DTO;

import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.report.ReportSeverity;

public record ComplianceReportResponse(
        Period period,
        String scopeDescription,
        boolean evaluationsAvailable,
        long totalEvaluations,
        long compliant,
        long nonCompliant,
        long notApplicable,
        long errors,
        Double compliancePercentage,
        List<PolicyCompliance> byPolicy,
        List<AssetCompliance> byAsset,
        List<RuleViolation> topViolations) {

    public record PolicyCompliance(Long policyId, String policyName, long compliant, long nonCompliant,
                                   long notApplicable, long errors, Double compliancePercentage) {
    }

    public record AssetCompliance(UUID assetId, String ipAddress, String hostname, long compliant,
                                  long nonCompliant, long notApplicable, long errors, Double compliancePercentage) {
    }

    public record RuleViolation(Long ruleId, String ruleName, String policyName, ReportSeverity severity,
                                long affectedAssets) {
    }
}
