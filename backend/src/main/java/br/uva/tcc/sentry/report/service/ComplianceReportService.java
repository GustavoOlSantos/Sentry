package br.uva.tcc.sentry.report.service;

import static br.uva.tcc.sentry.report.service.ReportFormatUtils.describeScope;
import static br.uva.tcc.sentry.report.service.ReportFormatUtils.percentage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.policy.domain.PolicyEvaluationStatus;
import br.uva.tcc.sentry.report.DTO.ComplianceReportResponse;
import br.uva.tcc.sentry.report.DTO.ComplianceReportResponse.AssetCompliance;
import br.uva.tcc.sentry.report.DTO.ComplianceReportResponse.PolicyCompliance;
import br.uva.tcc.sentry.report.DTO.ComplianceReportResponse.RuleViolation;
import br.uva.tcc.sentry.report.DTO.Period;
import br.uva.tcc.sentry.report.ReportFilter;
import br.uva.tcc.sentry.report.repository.ReportRepository;
import br.uva.tcc.sentry.report.repository.ReportRepository.AssetEvaluationRow;
import br.uva.tcc.sentry.report.repository.ReportRepository.EvaluationCount;
import br.uva.tcc.sentry.report.repository.ReportRepository.PolicyEvaluationRow;
import br.uva.tcc.sentry.report.repository.ReportRepository.RuleViolationRow;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComplianceReportService {

    private final ReportRepository repository;

    public ComplianceReportResponse report(ReportFilter filter) {
        Map<PolicyEvaluationStatus, Long> counts = new EnumMap<>(PolicyEvaluationStatus.class);
        for (EvaluationCount row : repository.evaluationCounts(filter)) {
            counts.put(row.status(), row.count());
        }

        long compliant = counts.getOrDefault(PolicyEvaluationStatus.COMPLIANT, 0L);
        long nonCompliant = counts.getOrDefault(PolicyEvaluationStatus.NON_COMPLIANT, 0L);
        long notApplicable = counts.getOrDefault(PolicyEvaluationStatus.NOT_APPLICABLE, 0L);
        long errors = counts.getOrDefault(PolicyEvaluationStatus.ERROR, 0L);
        long total = compliant + nonCompliant + notApplicable + errors;
        long applicableConcluded = compliant + nonCompliant;

        Double compliancePct = applicableConcluded > 0 ? percentage(compliant, applicableConcluded) : null;

        // Por política
        Map<Long, String> policyNames = new LinkedHashMap<>();
        Map<Long, Map<PolicyEvaluationStatus, Long>> policyStats = new LinkedHashMap<>();
        for (PolicyEvaluationRow row : repository.evaluationsByPolicy(filter)) {
            policyNames.put(row.policyId(), row.policyName());
            policyStats.computeIfAbsent(row.policyId(), k -> new EnumMap<>(PolicyEvaluationStatus.class))
                    .merge(row.status(), row.count(), Long::sum);
        }

        List<PolicyCompliance> byPolicy = new ArrayList<>();
        for (var entry : policyStats.entrySet()) {
            Long id = entry.getKey();
            var st = entry.getValue();
            long c = st.getOrDefault(PolicyEvaluationStatus.COMPLIANT, 0L);
            long nc = st.getOrDefault(PolicyEvaluationStatus.NON_COMPLIANT, 0L);
            long na = st.getOrDefault(PolicyEvaluationStatus.NOT_APPLICABLE, 0L);
            long er = st.getOrDefault(PolicyEvaluationStatus.ERROR, 0L);
            Double pct = (c + nc) > 0 ? percentage(c, c + nc) : null;
            byPolicy.add(new PolicyCompliance(id, policyNames.get(id), c, nc, na, er, pct));
        }
        byPolicy.sort(Comparator.comparing(PolicyCompliance::policyName, String.CASE_INSENSITIVE_ORDER));

        // Por ativo
        record AssetInfo(String ip, String hostname) {}
        Map<UUID, AssetInfo> assetInfos = new LinkedHashMap<>();
        Map<UUID, Map<PolicyEvaluationStatus, Long>> assetStats = new LinkedHashMap<>();
        for (AssetEvaluationRow row : repository.evaluationsByAsset(filter)) {
            assetInfos.put(row.assetId(), new AssetInfo(row.ipAddress(), row.hostname()));
            assetStats.computeIfAbsent(row.assetId(), k -> new EnumMap<>(PolicyEvaluationStatus.class))
                    .merge(row.status(), row.count(), Long::sum);
        }

        List<AssetCompliance> byAsset = new ArrayList<>();
        for (var entry : assetStats.entrySet()) {
            UUID id = entry.getKey();
            AssetInfo info = assetInfos.get(id);
            var st = entry.getValue();
            long c = st.getOrDefault(PolicyEvaluationStatus.COMPLIANT, 0L);
            long nc = st.getOrDefault(PolicyEvaluationStatus.NON_COMPLIANT, 0L);
            long na = st.getOrDefault(PolicyEvaluationStatus.NOT_APPLICABLE, 0L);
            long er = st.getOrDefault(PolicyEvaluationStatus.ERROR, 0L);
            Double pct = (c + nc) > 0 ? percentage(c, c + nc) : null;
            byAsset.add(new AssetCompliance(id, info.ip(), info.hostname(), c, nc, na, er, pct));
        }
        byAsset.sort(Comparator.comparingLong(AssetCompliance::nonCompliant).reversed()
                .thenComparing(AssetCompliance::ipAddress));

        // Top violações
        List<RuleViolation> topViolations = new ArrayList<>();
        for (RuleViolationRow row : repository.topViolations(filter, 10)) {
            topViolations.add(new RuleViolation(row.ruleId(), row.ruleName(), row.policyName(),
                    row.severity(), row.affectedAssets()));
        }

        return new ComplianceReportResponse(
                Period.of(filter),
                describeScope(filter, repository),
                total > 0,
                total,
                compliant,
                nonCompliant,
                notApplicable,
                errors,
                compliancePct,
                byPolicy,
                byAsset,
                topViolations);
    }
}
