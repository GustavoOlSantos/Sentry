package br.uva.tcc.sentry.report.service;

import static br.uva.tcc.sentry.report.service.ReportFormatUtils.describeScope;
import static br.uva.tcc.sentry.report.service.ReportFormatUtils.formatDuration;
import static br.uva.tcc.sentry.report.service.ReportFormatUtils.round2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.DTO.MttrReportResponse;
import br.uva.tcc.sentry.report.DTO.MttrReportResponse.SeverityMttr;
import br.uva.tcc.sentry.report.DTO.MttrReportResponse.TypeMttr;
import br.uva.tcc.sentry.report.DTO.Period;
import br.uva.tcc.sentry.report.ReportFilter;
import br.uva.tcc.sentry.report.ReportSeverity;
import br.uva.tcc.sentry.report.repository.ReportRepository;
import br.uva.tcc.sentry.report.repository.ReportRepository.MttrGroupRow;
import br.uva.tcc.sentry.report.repository.ReportRepository.MttrStats;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MttrReportService {

    private final ReportRepository repository;

    public MttrReportResponse report(ReportFilter filter) {
        MttrStats stats = repository.mttrStats(filter);
        long completed = stats.count();
        long pending = repository.pendingFindings(filter);

        Long avgSeconds = completed > 0 && stats.averageSeconds() != null ? Math.round(stats.averageSeconds()) : null;
        Double avgHours = completed > 0 && stats.averageSeconds() != null ? round2(stats.averageSeconds() / 3600.0) : null;

        Double medianRaw = completed > 0 ? repository.mttrMedianSeconds(filter) : null;
        Long medianSeconds = medianRaw != null ? Math.round(medianRaw) : null;

        Map<ReportSeverity, MttrGroupRow> sevMap = new HashMap<>();
        for (MttrGroupRow row : repository.mttrBySeverity(filter)) {
            ReportSeverity key = ReportSeverity.of(row.key());
            sevMap.merge(key, row, (a, b) -> {
                long totalCount = a.count() + b.count();
                double weightedAvg = totalCount == 0 ? 0.0
                        : ((a.averageSeconds() * a.count()) + (b.averageSeconds() * b.count())) / totalCount;
                return new MttrGroupRow(key.name(), totalCount, weightedAvg);
            });
        }

        List<SeverityMttr> bySeverity = new ArrayList<>();
        for (ReportSeverity sev : ReportSeverity.ORDERED) {
            MttrGroupRow row = sevMap.get(sev);
            if (row == null || row.count() == 0 || row.averageSeconds() == null) {
                bySeverity.add(new SeverityMttr(sev, null, null, null, 0L));
            } else {
                long s = Math.round(row.averageSeconds());
                bySeverity.add(new SeverityMttr(sev, s, round2(row.averageSeconds() / 3600.0), formatDuration(s), row.count()));
            }
        }

        Map<FindingType, MttrGroupRow> typeMap = new HashMap<>();
        for (MttrGroupRow row : repository.mttrByType(filter)) {
            typeMap.put(FindingType.valueOf(row.key()), row);
        }

        List<TypeMttr> byType = new ArrayList<>();
        for (FindingType type : FindingType.values()) {
            MttrGroupRow row = typeMap.get(type);
            if (row == null || row.count() == 0 || row.averageSeconds() == null) {
                byType.add(new TypeMttr(type, null, null, null, 0L));
            } else {
                long s = Math.round(row.averageSeconds());
                byType.add(new TypeMttr(type, s, round2(row.averageSeconds() / 3600.0), formatDuration(s), row.count()));
            }
        }

        return new MttrReportResponse(
                Period.of(filter),
                describeScope(filter, repository),
                avgSeconds,
                avgHours,
                formatDuration(avgSeconds),
                medianSeconds,
                formatDuration(medianSeconds),
                stats.minSeconds(),
                formatDuration(stats.minSeconds()),
                stats.maxSeconds(),
                formatDuration(stats.maxSeconds()),
                completed,
                pending,
                bySeverity,
                byType);
    }
}
