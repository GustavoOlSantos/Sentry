package br.uva.tcc.sentry.report.service;

import static br.uva.tcc.sentry.report.service.ReportFormatUtils.describeScope;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.DTO.Period;
import br.uva.tcc.sentry.report.DTO.RiskEvolutionResponse;
import br.uva.tcc.sentry.report.DTO.RiskEvolutionResponse.RiskEvolutionPoint;
import br.uva.tcc.sentry.report.DTO.RiskEvolutionResponse.ScanEvolutionPoint;
import br.uva.tcc.sentry.report.EvolutionGroupBy;
import br.uva.tcc.sentry.report.ReportFilter;
import br.uva.tcc.sentry.report.ReportSeverity;
import br.uva.tcc.sentry.report.repository.ReportRepository;
import br.uva.tcc.sentry.report.repository.ReportRepository.DayCount;
import br.uva.tcc.sentry.report.repository.ReportRepository.DayState;
import br.uva.tcc.sentry.report.repository.ReportRepository.ScanCount;
import br.uva.tcc.sentry.report.repository.ReportRepository.ScanObserved;
import br.uva.tcc.sentry.report.repository.ReportRepository.ScanRow;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskEvolutionService {

    private static final Set<FindingStatus> PENDING = EnumSet.of(
            FindingStatus.OPEN,
            FindingStatus.IN_PROGRESS,
            FindingStatus.FAILED_VALIDATION,
            FindingStatus.RESOLVED);

    private final ReportRepository repository;

    public RiskEvolutionResponse evolution(ReportFilter filter, EvolutionGroupBy groupBy) {
        EvolutionGroupBy mode = groupBy != null ? groupBy : EvolutionGroupBy.DAY;
        List<String> limitations = buildLimitations(filter);

        if (mode == EvolutionGroupBy.SCAN) {
            return new RiskEvolutionResponse(
                    Period.of(filter),
                    EvolutionGroupBy.SCAN,
                    describeScope(filter, repository),
                    List.of(),
                    buildScanPoints(filter),
                    limitations);
        }

        return new RiskEvolutionResponse(
                Period.of(filter),
                EvolutionGroupBy.DAY,
                describeScope(filter, repository),
                buildDayPoints(filter),
                List.of(),
                limitations);
    }

    private List<RiskEvolutionPoint> buildDayPoints(ReportFilter filter) {
        Map<LocalDate, Long> detectedMap = new HashMap<>();
        for (DayCount row : repository.detectedPerDay(filter)) {
            detectedMap.put(row.date(), row.count());
        }

        Map<LocalDate, Long> concludedMap = new HashMap<>();
        for (DayCount row : repository.concludedPerDay(filter)) {
            concludedMap.put(row.date(), row.count());
        }

        Map<LocalDate, Long> pendingTotalMap = new HashMap<>();
        Map<LocalDate, Long> acceptedMap = new HashMap<>();
        Map<LocalDate, Map<ReportSeverity, Long>> pendingBySevMap = new HashMap<>();

        for (DayState row : repository.stateAtEndOfDay(filter)) {
            if (row.status() == FindingStatus.JUSTIFIED) {
                acceptedMap.merge(row.date(), row.count(), Long::sum);
            } else if (PENDING.contains(row.status())) {
                pendingTotalMap.merge(row.date(), row.count(), Long::sum);
                pendingBySevMap.computeIfAbsent(row.date(), k -> emptySeverityMap())
                        .merge(row.severity(), row.count(), Long::sum);
            }
        }

        List<RiskEvolutionPoint> points = new ArrayList<>();
        LocalDate cursor = filter.from();
        while (!cursor.isAfter(filter.to())) {
            Map<ReportSeverity, Long> sevMap = pendingBySevMap.getOrDefault(cursor, emptySeverityMap());
            points.add(new RiskEvolutionPoint(
                    cursor,
                    detectedMap.getOrDefault(cursor, 0L),
                    concludedMap.getOrDefault(cursor, 0L),
                    pendingTotalMap.getOrDefault(cursor, 0L),
                    acceptedMap.getOrDefault(cursor, 0L),
                    sevMap));
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    private List<ScanEvolutionPoint> buildScanPoints(ReportFilter filter) {
        List<ScanRow> scans = repository.scansInPeriod(filter);
        if (scans.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> totalObserved = new HashMap<>();
        Map<UUID, Long> newObserved = new HashMap<>();
        Map<UUID, Map<ReportSeverity, Long>> sevByScan = new HashMap<>();
        Map<UUID, Map<FindingType, Long>> typeByScan = new HashMap<>();

        for (ScanObserved row : repository.observedPerScan(filter)) {
            totalObserved.merge(row.scanId(), row.count(), Long::sum);
            newObserved.merge(row.scanId(), row.newCount(), Long::sum);
            sevByScan.computeIfAbsent(row.scanId(), k -> emptySeverityMap())
                    .merge(row.severity(), row.count(), Long::sum);
            typeByScan.computeIfAbsent(row.scanId(), k -> emptyTypeMap())
                    .merge(row.type(), row.count(), Long::sum);
        }

        Map<UUID, Long> validatedMap = new HashMap<>();
        for (ScanCount row : repository.validatedPerScan(filter)) {
            validatedMap.put(row.scanId(), row.count());
        }

        Map<String, Long> previousByTarget = new LinkedHashMap<>();
        List<ScanEvolutionPoint> points = new ArrayList<>();

        for (ScanRow scan : scans) {
            long observed = totalObserved.getOrDefault(scan.scanId(), 0L);
            long fresh = newObserved.getOrDefault(scan.scanId(), 0L);
            long carried = Math.max(0L, observed - fresh);
            long resolvedByScan = validatedMap.getOrDefault(scan.scanId(), 0L);

            Long prev = previousByTarget.get(scan.target());
            Long diff = prev != null ? observed - prev : null;
            previousByTarget.put(scan.target(), observed);

            points.add(new ScanEvolutionPoint(
                    scan.scanId(),
                    scan.groupId(),
                    scan.target(),
                    scan.status(),
                    scan.executedAt(),
                    observed,
                    fresh,
                    carried,
                    resolvedByScan,
                    observed,
                    diff,
                    sevByScan.getOrDefault(scan.scanId(), emptySeverityMap()),
                    typeByScan.getOrDefault(scan.scanId(), emptyTypeMap())));
        }
        return points;
    }

    private List<String> buildLimitations(ReportFilter filter) {
        List<String> notes = new ArrayList<>();
        long backfilled = repository.findingsWithBackfilledHistory(filter);
        if (backfilled > 0) {
            notes.add(backfilled + " finding(s) são anteriores ao registro do histórico de status: só a detecção e a "
                    + "validação são conhecidas (a partir dos timestamps). Mudanças intermediárias, como justificativas, "
                    + "não foram registradas, e esses findings aparecem como pendentes até serem validados.");
        }
        return notes;
    }

    private static Map<ReportSeverity, Long> emptySeverityMap() {
        Map<ReportSeverity, Long> map = new EnumMap<>(ReportSeverity.class);
        for (ReportSeverity sev : ReportSeverity.ORDERED) {
            map.put(sev, 0L);
        }
        return map;
    }

    private static Map<FindingType, Long> emptyTypeMap() {
        Map<FindingType, Long> map = new EnumMap<>(FindingType.class);
        for (FindingType type : FindingType.values()) {
            map.put(type, 0L);
        }
        return map;
    }
}
