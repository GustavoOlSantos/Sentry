package br.uva.tcc.sentry.report.service;

import static br.uva.tcc.sentry.report.service.ReportFormatUtils.describeScope;
import static br.uva.tcc.sentry.report.service.ReportFormatUtils.percentage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.DTO.ComplianceReportResponse;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse.AssetFindings;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse.ColumnCount;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse.SeverityCount;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse.StatusCount;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse.TypeCount;
import br.uva.tcc.sentry.report.DTO.FullReport;
import br.uva.tcc.sentry.report.DTO.MttrReportResponse;
import br.uva.tcc.sentry.report.DTO.Period;
import br.uva.tcc.sentry.report.DTO.RelevantFindingsResponse;
import br.uva.tcc.sentry.report.DTO.RelevantFindingsResponse.RelevantFinding;
import br.uva.tcc.sentry.report.DTO.ReportSummaryResponse;
import br.uva.tcc.sentry.report.DTO.ReportSummaryResponse.ComplianceSummary;
import br.uva.tcc.sentry.report.DTO.ReportSummaryResponse.MttrSummary;
import br.uva.tcc.sentry.report.DTO.RiskEvolutionResponse;
import br.uva.tcc.sentry.report.EvolutionGroupBy;
import br.uva.tcc.sentry.report.KanbanColumn;
import br.uva.tcc.sentry.report.ReportFilter;
import br.uva.tcc.sentry.report.ReportSeverity;
import br.uva.tcc.sentry.report.repository.ReportRepository;
import br.uva.tcc.sentry.report.repository.ReportRepository.AssetRow;
import br.uva.tcc.sentry.report.repository.ReportRepository.FindingGroupRow;
import br.uva.tcc.sentry.report.repository.ReportRepository.PeriodFacts;
import br.uva.tcc.sentry.report.repository.ReportRepository.RelevantRow;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    public static final int DEFAULT_RELEVANT = 25;
    private static final int MAX_RELEVANT = 100;
    private static final int TOP_ASSETS_LIMIT = 15;

    private final ReportRepository repository;
    private final RiskEvolutionService evolutionService;
    private final MttrReportService mttrService;
    private final ComplianceReportService complianceService;

    public ReportSummaryResponse summary(ReportFilter filter) {
        List<FindingGroupRow> groups = repository.findingGroups(filter);
        PeriodFacts facts = repository.periodFacts(filter);
        long assetsAnalyzed = repository.assetsAnalyzed(filter);
        MttrReportResponse mttr = mttrService.report(filter);
        ComplianceReportResponse compliance = complianceService.report(filter);

        long total = 0;
        long open = 0, inProgress = 0, resolved = 0, validated = 0, acceptedRisk = 0;
        long critical = 0, high = 0, pendingCritical = 0, pendingHigh = 0;

        for (FindingGroupRow row : groups) {
            long c = row.count();
            total += c;

            switch (row.status()) {
                case OPEN -> open += c;
                case IN_PROGRESS, FAILED_VALIDATION -> inProgress += c;
                case RESOLVED -> resolved += c;
                case VALIDATED -> validated += c;
                case JUSTIFIED -> acceptedRisk += c;
            }

            boolean isPending = row.status() != FindingStatus.VALIDATED && row.status() != FindingStatus.JUSTIFIED;
            if (row.severity() == ReportSeverity.CRITICAL) {
                critical += c;
                if (isPending) pendingCritical += c;
            } else if (row.severity() == ReportSeverity.HIGH) {
                high += c;
                if (isPending) pendingHigh += c;
            }
        }

        List<String> limitations = new ArrayList<>();
        limitations.add("Contagens por status refletem o status atual dos findings do período; a evolução histórica usa o histórico de transições.");
        limitations.add("Findings JUSTIFIED são riscos aceitos e não são considerados corrigidos.");
        limitations.add("O compliance considera apenas regras de política cadastradas; as checagens fixas do Sentry não geram avaliações.");
        if (repository.findingsWithBackfilledHistory(filter) > 0) {
            limitations.add("Há findings anteriores ao registro do histórico de status; a evolução diária deles usa apenas os timestamps de detecção e validação.");
        }

        return new ReportSummaryResponse(
                Period.of(filter),
                describeScope(filter, repository),
                total,
                facts.detectedInPeriod(),
                facts.carriedOver(),
                facts.concludedInPeriod(),
                open,
                inProgress,
                resolved,
                validated,
                resolved + validated,
                acceptedRisk,
                critical,
                high,
                pendingCritical,
                pendingHigh,
                assetsAnalyzed,
                facts.assetsWithFindings(),
                new MttrSummary(mttr.averageSeconds(), mttr.averageLabel(), mttr.completedFindings()),
                new ComplianceSummary(compliance.compliancePercentage(), compliance.totalEvaluations()),
                limitations);
    }

    public FindingsDistributionResponse distribution(ReportFilter filter) {
        List<FindingGroupRow> groups = repository.findingGroups(filter);

        Map<ReportSeverity, Long> sevCounts = new EnumMap<>(ReportSeverity.class);
        Map<FindingType, Long> typeCounts = new EnumMap<>(FindingType.class);
        Map<FindingStatus, Long> statusCounts = new EnumMap<>(FindingStatus.class);
        Map<KanbanColumn, Long> columnCounts = new EnumMap<>(KanbanColumn.class);

        long total = 0;
        for (FindingGroupRow row : groups) {
            long c = row.count();
            total += c;
            sevCounts.merge(row.severity(), c, Long::sum);
            typeCounts.merge(row.type(), c, Long::sum);
            statusCounts.merge(row.status(), c, Long::sum);
            columnCounts.merge(KanbanColumn.of(row.status()), c, Long::sum);
        }

        List<SeverityCount> bySeverity = new ArrayList<>();
        for (ReportSeverity sev : ReportSeverity.ORDERED) {
            long c = sevCounts.getOrDefault(sev, 0L);
            bySeverity.add(new SeverityCount(sev, c, percentage(c, total)));
        }

        List<TypeCount> byType = new ArrayList<>();
        for (FindingType type : FindingType.values()) {
            long c = typeCounts.getOrDefault(type, 0L);
            byType.add(new TypeCount(type, c, percentage(c, total)));
        }

        List<StatusCount> byStatus = new ArrayList<>();
        for (FindingStatus status : FindingStatus.values()) {
            long c = statusCounts.getOrDefault(status, 0L);
            byStatus.add(new StatusCount(status, KanbanColumn.of(status), c, percentage(c, total)));
        }

        List<ColumnCount> byColumn = new ArrayList<>();
        for (KanbanColumn col : KanbanColumn.ORDERED) {
            long c = columnCounts.getOrDefault(col, 0L);
            byColumn.add(new ColumnCount(col, c, percentage(c, total)));
        }

        List<AssetFindings> topAssets = new ArrayList<>();
        for (AssetRow row : repository.topAssets(filter, TOP_ASSETS_LIMIT)) {
            topAssets.add(new AssetFindings(
                    row.assetId(),
                    row.ipAddress(),
                    row.hostname(),
                    row.total(),
                    row.bySeverity(),
                    row.pending(),
                    row.open(),
                    row.validated(),
                    row.acceptedRisk()));
        }

        return new FindingsDistributionResponse(
                Period.of(filter),
                describeScope(filter, repository),
                total,
                bySeverity,
                byType,
                byStatus,
                byColumn,
                topAssets);
    }

    public RelevantFindingsResponse relevantFindings(ReportFilter filter, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_RELEVANT));
        long totalInScope = repository.findingGroups(filter).stream().mapToLong(FindingGroupRow::count).sum();

        List<RelevantFinding> list = new ArrayList<>();
        for (RelevantRow row : repository.relevantFindings(filter, safeLimit)) {
            list.add(new RelevantFinding(
                    row.id(),
                    row.title(),
                    row.code(),
                    row.severity(),
                    row.type(),
                    row.status(),
                    row.assetIp(),
                    row.assetHostname(),
                    row.port(),
                    row.protocol(),
                    row.detectedAt()));
        }

        return new RelevantFindingsResponse(Period.of(filter), safeLimit, totalInScope, list);
    }

    public FullReport fullReport(ReportFilter filter) {
        LocalDateTime now = LocalDateTime.now();
        String reportId = "SENTRY-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(now);
        Period period = Period.of(filter);
        String scope = describeScope(filter, repository);

        ReportSummaryResponse summary = summary(filter);
        RiskEvolutionResponse evolution = evolutionService.evolution(filter, EvolutionGroupBy.DAY);
        FindingsDistributionResponse distribution = distribution(filter);
        MttrReportResponse mttr = mttrService.report(filter);
        ComplianceReportResponse compliance = complianceService.report(filter);
        RelevantFindingsResponse relevant = relevantFindings(filter, DEFAULT_RELEVANT);

        return new FullReport(
                reportId,
                now,
                period,
                scope,
                summary,
                evolution,
                distribution,
                mttr,
                compliance,
                relevant);
    }
}
