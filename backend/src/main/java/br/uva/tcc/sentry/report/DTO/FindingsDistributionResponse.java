package br.uva.tcc.sentry.report.DTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.KanbanColumn;
import br.uva.tcc.sentry.report.ReportSeverity;

public record FindingsDistributionResponse(
        Period period,
        String scopeDescription,
        long total,
        List<SeverityCount> bySeverity,
        List<TypeCount> byType,
        List<StatusCount> byStatus,
        List<ColumnCount> byKanbanColumn,
        List<AssetFindings> topAssets) {

    public record SeverityCount(ReportSeverity severity, long count, double percentage) {
    }

    public record TypeCount(FindingType type, long count, double percentage) {
    }

    public record StatusCount(FindingStatus status, KanbanColumn column, long count, double percentage) {
    }

    public record ColumnCount(KanbanColumn column, long count, double percentage) {
    }

    public record AssetFindings(
            UUID assetId,
            String ipAddress,
            String hostname,
            long total,
            Map<ReportSeverity, Long> bySeverity,
            long pending,
            long open,
            long validated,
            long acceptedRisk) {
    }
}
