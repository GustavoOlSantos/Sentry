package br.uva.tcc.sentry.report.repository;

import static br.uva.tcc.sentry.report.repository.Rows.date;
import static br.uva.tcc.sentry.report.repository.Rows.dateTime;
import static br.uva.tcc.sentry.report.repository.Rows.dbl;
import static br.uva.tcc.sentry.report.repository.Rows.intOrNull;
import static br.uva.tcc.sentry.report.repository.Rows.lng;
import static br.uva.tcc.sentry.report.repository.Rows.lngOrNull;
import static br.uva.tcc.sentry.report.repository.Rows.str;
import static br.uva.tcc.sentry.report.repository.Rows.uuid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.policy.domain.PolicyEvaluationStatus;
import br.uva.tcc.sentry.report.ReportFilter;
import br.uva.tcc.sentry.report.ReportSeverity;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.shared.domain.Severity;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private static final String PENDING_STATUSES = "'OPEN','IN_PROGRESS','FAILED_VALIDATION','RESOLVED'";

    private static final String SCOPE =
            " f.detected_at < :toExclusive AND (f.validated_at IS NULL OR f.validated_at >= :fromStart)";

    private final SqlRunner sql;

    public record FindingGroupRow(ReportSeverity severity, FindingType type, FindingStatus status, long count) {
    }

    public record PeriodFacts(long detectedInPeriod, long carriedOver, long concludedInPeriod, long assetsWithFindings) {
    }

    public record AssetRow(UUID assetId, String ipAddress, String hostname, long total,
                           Map<ReportSeverity, Long> bySeverity, long pending, long open, long validated,
                           long acceptedRisk) {
    }

    public record RelevantRow(UUID id, String title, String code, ReportSeverity severity, FindingType type,
                              FindingStatus status, String assetIp, String assetHostname, Integer port,
                              String protocol, LocalDateTime detectedAt) {
    }

    public record DayCount(LocalDate date, long count) {
    }

    public record DayState(LocalDate date, ReportSeverity severity, FindingStatus status, long count) {
    }

    public record ScanRow(UUID scanId, UUID groupId, String target, ScanStatus status, LocalDateTime executedAt) {
    }

    public record ScanObserved(UUID scanId, ReportSeverity severity, FindingType type, long count, long newCount) {
    }

    public record ScanCount(UUID scanId, long count) {
    }

    public record MttrStats(long count, Double averageSeconds, Long minSeconds, Long maxSeconds) {
    }

    public record MttrGroupRow(String key, long count, Double averageSeconds) {
    }

    public record EvaluationCount(PolicyEvaluationStatus status, long count) {
    }

    public record PolicyEvaluationRow(Long policyId, String policyName, PolicyEvaluationStatus status, long count) {
    }

    public record AssetEvaluationRow(UUID assetId, String ipAddress, String hostname,
                                     PolicyEvaluationStatus status, long count) {
    }

    public record RuleViolationRow(Long ruleId, String ruleName, String policyName, ReportSeverity severity,
                                   long affectedAssets) {
    }

    private static final class Fragment {
        final StringBuilder sql = new StringBuilder();
        final Map<String, Object> params = new LinkedHashMap<>();
    }

    private static Map<String, Object> basePeriodParams(ReportFilter filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fromStart", filter.fromStart());
        params.put("toExclusive", filter.toExclusive());
        return params;
    }

    private static Fragment findingFilters(ReportFilter filter, boolean withStatus) {
        Fragment fragment = new Fragment();

        if (filter.assetId() != null) {
            fragment.sql.append(" AND f.asset_id = :assetId");
            fragment.params.put("assetId", filter.assetId());
        }
        if (filter.scanId() != null) {
            fragment.sql.append(" AND EXISTS (SELECT 1 FROM scan_finding sf_s WHERE sf_s.finding_id = f.id AND sf_s.scan_id = :scanId)");
            fragment.params.put("scanId", filter.scanId());
        }
        if (filter.scanGroupId() != null) {
            fragment.sql.append(" AND EXISTS (SELECT 1 FROM scan_finding sf_g JOIN scan s_g ON s_g.id = sf_g.scan_id"
                    + " WHERE sf_g.finding_id = f.id AND s_g.exec_group_id = :scanGroupId)");
            fragment.params.put("scanGroupId", filter.scanGroupId());
        }
        if (filter.severity() != null) {
            if (filter.severity() == Severity.INFO || filter.severity() == Severity.NONE) {
                fragment.sql.append(" AND (f.severity IS NULL OR f.severity IN ('INFO','NONE'))");
            } else {
                fragment.sql.append(" AND f.severity = :severity");
                fragment.params.put("severity", filter.severity().name());
            }
        }
        if (filter.type() != null) {
            fragment.sql.append(" AND f.type = :type");
            fragment.params.put("type", filter.type().name());
        }
        if (withStatus && filter.status() != null) {
            fragment.sql.append(" AND f.status = :status");
            fragment.params.put("status", filter.status().name());
        }
        return fragment;
    }

    private static Fragment scanFilters(ReportFilter filter) {
        Fragment fragment = new Fragment();

        fragment.sql.append(" COALESCE(s.started_at, s.created_at) >= :fromStart")
                .append(" AND COALESCE(s.started_at, s.created_at) < :toExclusive");

        if (filter.scanId() != null) {
            fragment.sql.append(" AND s.id = :scanId");
            fragment.params.put("scanId", filter.scanId());
        }
        if (filter.scanGroupId() != null) {
            fragment.sql.append(" AND s.exec_group_id = :scanGroupId");
            fragment.params.put("scanGroupId", filter.scanGroupId());
        }
        if (filter.assetId() != null) {
            fragment.sql.append(" AND EXISTS (SELECT 1 FROM scan_asset sa_f WHERE sa_f.scan_id = s.id AND sa_f.asset_id = :assetId)");
            fragment.params.put("assetId", filter.assetId());
        }
        return fragment;
    }

    private List<Object[]> run(String statement, Map<String, Object> params, Fragment... fragments) {
        Map<String, Object> all = new LinkedHashMap<>(params);
        for (Fragment fragment : fragments) {
            all.putAll(fragment.params);
        }
        return sql.query(statement, all);
    }

    public List<FindingGroupRow> findingGroups(ReportFilter filter) {
        Fragment filters = findingFilters(filter, true);

        String statement = "SELECT COALESCE(f.severity, 'NONE'), f.type, f.status, COUNT(*)"
                + " FROM finding f WHERE" + SCOPE + filters.sql
                + " GROUP BY COALESCE(f.severity, 'NONE'), f.type, f.status";

        List<FindingGroupRow> rows = new ArrayList<>();
        for (Object[] r : run(statement, basePeriodParams(filter), filters)) {
            rows.add(new FindingGroupRow(ReportSeverity.of(str(r[0])), FindingType.valueOf(str(r[1])),
                    FindingStatus.valueOf(str(r[2])), lng(r[3])));
        }
        return rows;
    }

    public PeriodFacts periodFacts(ReportFilter filter) {
        Fragment filters = findingFilters(filter, true);

        String statement = "SELECT COALESCE(SUM(f.detected_at >= :fromStart), 0),"
                + " COALESCE(SUM(f.detected_at < :fromStart), 0),"
                + " COALESCE(SUM(f.validated_at >= :fromStart AND f.validated_at < :toExclusive), 0),"
                + " COUNT(DISTINCT f.asset_id)"
                + " FROM finding f WHERE" + SCOPE + filters.sql;

        Object[] r = run(statement, basePeriodParams(filter), filters).get(0);
        return new PeriodFacts(lng(r[0]), lng(r[1]), lng(r[2]), lng(r[3]));
    }

    public long assetsAnalyzed(ReportFilter filter) {
        Fragment filters = scanFilters(filter);

        String statement = "SELECT COUNT(DISTINCT sa.asset_id) FROM scan_asset sa JOIN scan s ON s.id = sa.scan_id WHERE"
                + filters.sql
                + (filter.assetId() != null ? " AND sa.asset_id = :assetId" : "");

        return lng(run(statement, basePeriodParams(filter), filters).get(0)[0]);
    }

    public List<AssetRow> topAssets(ReportFilter filter, int limit) {
        Fragment filters = findingFilters(filter, true);

        String statement = "SELECT a.id, a.ip_address, a.hostname, COUNT(*),"
                + " SUM(f.severity = 'CRITICAL'), SUM(f.severity = 'HIGH'), SUM(f.severity = 'MEDIUM'), SUM(f.severity = 'LOW'),"
                + " SUM(f.severity IS NULL OR f.severity IN ('INFO','NONE')),"
                + " SUM(f.status IN (" + PENDING_STATUSES + ")), SUM(f.status = 'OPEN'),"
                + " SUM(f.status = 'VALIDATED'), SUM(f.status = 'JUSTIFIED')"
                + " FROM finding f JOIN asset a ON a.id = f.asset_id WHERE" + SCOPE + filters.sql
                + " GROUP BY a.id, a.ip_address, a.hostname"
                + " ORDER BY SUM(f.severity = 'CRITICAL') DESC, SUM(f.severity = 'HIGH') DESC, COUNT(*) DESC, a.ip_address"
                + " LIMIT :limit";

        Map<String, Object> params = basePeriodParams(filter);
        params.put("limit", limit);

        List<AssetRow> rows = new ArrayList<>();
        for (Object[] r : run(statement, params, filters)) {
            Map<ReportSeverity, Long> bySeverity = new EnumMap<>(ReportSeverity.class);
            bySeverity.put(ReportSeverity.CRITICAL, lng(r[4]));
            bySeverity.put(ReportSeverity.HIGH, lng(r[5]));
            bySeverity.put(ReportSeverity.MEDIUM, lng(r[6]));
            bySeverity.put(ReportSeverity.LOW, lng(r[7]));
            bySeverity.put(ReportSeverity.INFO, lng(r[8]));

            rows.add(new AssetRow(uuid(r[0]), str(r[1]), str(r[2]), lng(r[3]), bySeverity,
                    lng(r[9]), lng(r[10]), lng(r[11]), lng(r[12])));
        }
        return rows;
    }

    public List<RelevantRow> relevantFindings(ReportFilter filter, int limit) {
        Fragment filters = findingFilters(filter, true);

        String statement = "SELECT f.id, f.title, f.code, COALESCE(f.severity, 'NONE'), f.type, f.status,"
                + " a.ip_address, a.hostname, sv.port, sv.protocol, f.detected_at"
                + " FROM finding f JOIN asset a ON a.id = f.asset_id LEFT JOIN service sv ON sv.id = f.service_id"
                + " WHERE" + SCOPE + filters.sql
                + " ORDER BY CASE f.severity WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END,"
                + " CASE WHEN f.status IN ('VALIDATED','JUSTIFIED') THEN 1 ELSE 0 END,"
                + " f.detected_at DESC, f.id"
                + " LIMIT :limit";

        Map<String, Object> params = basePeriodParams(filter);
        params.put("limit", limit);

        List<RelevantRow> rows = new ArrayList<>();
        for (Object[] r : run(statement, params, filters)) {
            rows.add(new RelevantRow(uuid(r[0]), str(r[1]), str(r[2]), ReportSeverity.of(str(r[3])),
                    FindingType.valueOf(str(r[4])), FindingStatus.valueOf(str(r[5])), str(r[6]), str(r[7]),
                    intOrNull(r[8]), str(r[9]), dateTime(r[10])));
        }
        return rows;
    }

    public long findingsWithBackfilledHistory(ReportFilter filter) {
        Fragment filters = findingFilters(filter, false);

        String statement = "SELECT COUNT(DISTINCT f.id) FROM finding f"
                + " JOIN finding_status_history h ON h.finding_id = f.id AND h.origin = 'BACKFILL'"
                + " WHERE" + SCOPE + filters.sql;

        return lng(run(statement, basePeriodParams(filter), filters).get(0)[0]);
    }

    public List<DayCount> detectedPerDay(ReportFilter filter) {
        Fragment filters = findingFilters(filter, false);

        String statement = "SELECT DATE(f.detected_at), COUNT(*) FROM finding f"
                + " WHERE f.detected_at >= :fromStart AND f.detected_at < :toExclusive" + filters.sql
                + " GROUP BY DATE(f.detected_at)";

        return dayCounts(run(statement, basePeriodParams(filter), filters));
    }

    public List<DayCount> concludedPerDay(ReportFilter filter) {
        Fragment filters = findingFilters(filter, false);

        String statement = "SELECT DATE(h.changed_at), COUNT(*)"
                + " FROM finding_status_history h JOIN finding f ON f.id = h.finding_id"
                + " WHERE h.to_status = 'VALIDATED' AND h.changed_at >= :fromStart AND h.changed_at < :toExclusive"
                + filters.sql
                + " GROUP BY DATE(h.changed_at)";

        return dayCounts(run(statement, basePeriodParams(filter), filters));
    }

    private static List<DayCount> dayCounts(List<Object[]> rows) {
        List<DayCount> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new DayCount(date(r[0]), lng(r[1])));
        }
        return result;
    }

    public List<DayState> stateAtEndOfDay(ReportFilter filter) {
        Fragment filters = findingFilters(filter, false);

        String statement = "WITH RECURSIVE days AS ("
                + "  SELECT CAST(:fromDate AS DATE) AS d"
                + "  UNION ALL"
                + "  SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < CAST(:toDate AS DATE)"
                + "), scoped AS ("
                + "  SELECT f.id, COALESCE(f.severity, 'NONE') AS severity FROM finding f WHERE 1 = 1" + filters.sql
                + "), intervals AS ("
                + "  SELECT h.finding_id, h.to_status AS status, h.changed_at AS start_at,"
                + "         LEAD(h.changed_at) OVER (PARTITION BY h.finding_id ORDER BY h.changed_at, h.id) AS end_at"
                + "  FROM finding_status_history h JOIN scoped sc ON sc.id = h.finding_id"
                + ")"
                + " SELECT d.d, sc.severity, i.status, COUNT(*)"
                + " FROM days d"
                + " JOIN intervals i ON i.start_at < DATE_ADD(d.d, INTERVAL 1 DAY)"
                + "                 AND (i.end_at IS NULL OR i.end_at >= DATE_ADD(d.d, INTERVAL 1 DAY))"
                + " JOIN scoped sc ON sc.id = i.finding_id"
                + " GROUP BY d.d, sc.severity, i.status"
                + " ORDER BY d.d";

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fromDate", filter.from());
        params.put("toDate", filter.to());

        List<DayState> result = new ArrayList<>();
        for (Object[] r : run(statement, params, filters)) {
            result.add(new DayState(date(r[0]), ReportSeverity.of(str(r[1])), FindingStatus.valueOf(str(r[2])), lng(r[3])));
        }
        return result;
    }

    public List<ScanRow> scansInPeriod(ReportFilter filter) {
        Fragment scans = scanFilters(filter);

        String statement = "SELECT s.id, s.exec_group_id, s.target, s.status, COALESCE(s.started_at, s.created_at)"
                + " FROM scan s WHERE" + scans.sql
                + " ORDER BY COALESCE(s.started_at, s.created_at), s.id";

        List<ScanRow> rows = new ArrayList<>();
        for (Object[] r : run(statement, basePeriodParams(filter), scans)) {
            rows.add(new ScanRow(uuid(r[0]), uuid(r[1]), str(r[2]), ScanStatus.valueOf(str(r[3])), dateTime(r[4])));
        }
        return rows;
    }

    public List<ScanObserved> observedPerScan(ReportFilter filter) {
        Fragment scans = scanFilters(filter);
        Fragment findings = findingFilters(filter, false);

        String statement = "SELECT sf.scan_id, COALESCE(f.severity, 'NONE'), f.type, COUNT(*), SUM(f.scan_id = sf.scan_id)"
                + " FROM scan_finding sf JOIN finding f ON f.id = sf.finding_id JOIN scan s ON s.id = sf.scan_id"
                + " WHERE" + scans.sql + findings.sql
                + " GROUP BY sf.scan_id, COALESCE(f.severity, 'NONE'), f.type";

        List<ScanObserved> rows = new ArrayList<>();
        for (Object[] r : run(statement, basePeriodParams(filter), scans, findings)) {
            rows.add(new ScanObserved(uuid(r[0]), ReportSeverity.of(str(r[1])), FindingType.valueOf(str(r[2])),
                    lng(r[3]), lng(r[4])));
        }
        return rows;
    }

    public List<ScanCount> validatedPerScan(ReportFilter filter) {
        Fragment scans = scanFilters(filter);
        Fragment findings = findingFilters(filter, false);

        String statement = "SELECT h.scan_id, COUNT(*)"
                + " FROM finding_status_history h JOIN finding f ON f.id = h.finding_id JOIN scan s ON s.id = h.scan_id"
                + " WHERE h.to_status = 'VALIDATED' AND h.origin = 'SCAN' AND" + scans.sql + findings.sql
                + " GROUP BY h.scan_id";

        List<ScanCount> rows = new ArrayList<>();
        for (Object[] r : run(statement, basePeriodParams(filter), scans, findings)) {
            rows.add(new ScanCount(uuid(r[0]), lng(r[1])));
        }
        return rows;
    }

    private static final String MTTR_BASE =
            " f.status = 'VALIDATED' AND f.detected_at IS NOT NULL AND f.validated_at IS NOT NULL"
                    + " AND f.validated_at >= f.detected_at"
                    + " AND f.validated_at >= :fromStart AND f.validated_at < :toExclusive";

    private static final String SECONDS = "TIMESTAMPDIFF(SECOND, f.detected_at, f.validated_at)";

    public MttrStats mttrStats(ReportFilter filter) {
        Fragment filters = findingFilters(filter, false);

        String statement = "SELECT COUNT(*), AVG(" + SECONDS + "), MIN(" + SECONDS + "), MAX(" + SECONDS + ")"
                + " FROM finding f WHERE" + MTTR_BASE + filters.sql;

        Object[] r = run(statement, basePeriodParams(filter), filters).get(0);
        return new MttrStats(lng(r[0]), dbl(r[1]), lngOrNull(r[2]), lngOrNull(r[3]));
    }

    public Double mttrMedianSeconds(ReportFilter filter) {
        Fragment filters = findingFilters(filter, false);

        String statement = "SELECT AVG(t.sec) FROM ("
                + " SELECT " + SECONDS + " AS sec,"
                + "        ROW_NUMBER() OVER (ORDER BY " + SECONDS + ") AS rn,"
                + "        COUNT(*) OVER () AS cnt"
                + " FROM finding f WHERE" + MTTR_BASE + filters.sql
                + ") t WHERE t.rn IN (FLOOR((t.cnt + 1) / 2), CEIL((t.cnt + 1) / 2))";

        return dbl(run(statement, basePeriodParams(filter), filters).get(0)[0]);
    }

    public List<MttrGroupRow> mttrBySeverity(ReportFilter filter) {
        return mttrGrouped(filter, "COALESCE(f.severity, 'NONE')");
    }

    public List<MttrGroupRow> mttrByType(ReportFilter filter) {
        return mttrGrouped(filter, "f.type");
    }

    private List<MttrGroupRow> mttrGrouped(ReportFilter filter, String groupExpression) {
        Fragment filters = findingFilters(filter, false);

        String statement = "SELECT " + groupExpression + ", COUNT(*), AVG(" + SECONDS + ")"
                + " FROM finding f WHERE" + MTTR_BASE + filters.sql
                + " GROUP BY " + groupExpression;

        List<MttrGroupRow> rows = new ArrayList<>();
        for (Object[] r : run(statement, basePeriodParams(filter), filters)) {
            rows.add(new MttrGroupRow(str(r[0]), lng(r[1]), dbl(r[2])));
        }
        return rows;
    }

    public long pendingFindings(ReportFilter filter) {
        Fragment filters = findingFilters(filter, false);

        String statement = "SELECT COUNT(*) FROM finding f WHERE f.detected_at < :toExclusive"
                + " AND f.status IN (" + PENDING_STATUSES + ")" + filters.sql;

        return lng(run(statement, basePeriodParams(filter), filters).get(0)[0]);
    }

    private static Fragment complianceFilters(ReportFilter filter) {
        Fragment fragment = new Fragment();
        fragment.sql.append(" pe.evaluated_at >= :fromStart AND pe.evaluated_at < :toExclusive");

        if (filter.assetId() != null) {
            fragment.sql.append(" AND pe.asset_id = :assetId");
            fragment.params.put("assetId", filter.assetId());
        }
        if (filter.scanId() != null) {
            fragment.sql.append(" AND pe.scan_id = :scanId");
            fragment.params.put("scanId", filter.scanId());
        }
        if (filter.scanGroupId() != null) {
            fragment.sql.append(" AND s.exec_group_id = :scanGroupId");
            fragment.params.put("scanGroupId", filter.scanGroupId());
        }
        if (filter.policyId() != null) {
            fragment.sql.append(" AND r.policy_id = :policyId");
            fragment.params.put("policyId", filter.policyId());
        }
        return fragment;
    }

    private static String latestEvaluations(Fragment filters) {
        return "WITH latest AS ("
                + " SELECT pe.asset_id, pe.policy_rule_id, pe.status, r.policy_id,"
                + "        ROW_NUMBER() OVER (PARTITION BY pe.asset_id, pe.policy_rule_id"
                + "                           ORDER BY pe.evaluated_at DESC, pe.id DESC) AS rn"
                + " FROM policy_evaluation pe"
                + " JOIN policy_rule r ON r.id = pe.policy_rule_id"
                + " JOIN scan s ON s.id = pe.scan_id"
                + " WHERE" + filters.sql
                + ") ";
    }

    public List<EvaluationCount> evaluationCounts(ReportFilter filter) {
        Fragment filters = complianceFilters(filter);

        String statement = latestEvaluations(filters)
                + "SELECT status, COUNT(*) FROM latest WHERE rn = 1 GROUP BY status";

        List<EvaluationCount> rows = new ArrayList<>();
        for (Object[] r : run(statement, basePeriodParams(filter), filters)) {
            rows.add(new EvaluationCount(PolicyEvaluationStatus.valueOf(str(r[0])), lng(r[1])));
        }
        return rows;
    }

    public List<PolicyEvaluationRow> evaluationsByPolicy(ReportFilter filter) {
        Fragment filters = complianceFilters(filter);

        String statement = latestEvaluations(filters)
                + "SELECT l.policy_id, p.name, l.status, COUNT(*)"
                + " FROM latest l JOIN policy p ON p.id = l.policy_id WHERE l.rn = 1"
                + " GROUP BY l.policy_id, p.name, l.status";

        List<PolicyEvaluationRow> rows = new ArrayList<>();
        for (Object[] r : run(statement, basePeriodParams(filter), filters)) {
            rows.add(new PolicyEvaluationRow(lng(r[0]), str(r[1]), PolicyEvaluationStatus.valueOf(str(r[2])), lng(r[3])));
        }
        return rows;
    }

    public List<AssetEvaluationRow> evaluationsByAsset(ReportFilter filter) {
        Fragment filters = complianceFilters(filter);

        String statement = latestEvaluations(filters)
                + "SELECT a.id, a.ip_address, a.hostname, l.status, COUNT(*)"
                + " FROM latest l JOIN asset a ON a.id = l.asset_id WHERE l.rn = 1"
                + " GROUP BY a.id, a.ip_address, a.hostname, l.status";

        List<AssetEvaluationRow> rows = new ArrayList<>();
        for (Object[] r : run(statement, basePeriodParams(filter), filters)) {
            rows.add(new AssetEvaluationRow(uuid(r[0]), str(r[1]), str(r[2]),
                    PolicyEvaluationStatus.valueOf(str(r[3])), lng(r[4])));
        }
        return rows;
    }

    public List<RuleViolationRow> topViolations(ReportFilter filter, int limit) {
        Fragment filters = complianceFilters(filter);

        String statement = latestEvaluations(filters)
                + "SELECT r.id, r.name, p.name, COALESCE(r.severity, 'NONE'), COUNT(DISTINCT l.asset_id)"
                + " FROM latest l JOIN policy_rule r ON r.id = l.policy_rule_id JOIN policy p ON p.id = r.policy_id"
                + " WHERE l.rn = 1 AND l.status = 'NON_COMPLIANT'"
                + " GROUP BY r.id, r.name, p.name, COALESCE(r.severity, 'NONE')"
                + " ORDER BY COUNT(DISTINCT l.asset_id) DESC, r.name"
                + " LIMIT :limit";

        Map<String, Object> params = basePeriodParams(filter);
        params.put("limit", limit);

        List<RuleViolationRow> rows = new ArrayList<>();
        for (Object[] r : run(statement, params, filters)) {
            rows.add(new RuleViolationRow(lng(r[0]), str(r[1]), str(r[2]), ReportSeverity.of(str(r[3])), lng(r[4])));
        }
        return rows;
    }

    public Optional<String> assetLabel(UUID assetId) {
        List<Object[]> rows = sql.query("SELECT a.ip_address, a.hostname FROM asset a WHERE a.id = :id", Map.of("id", assetId));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        String ip = str(rows.get(0)[0]);
        String hostname = str(rows.get(0)[1]);
        return Optional.of(hostname == null || hostname.isBlank() ? ip : ip + " (" + hostname + ")");
    }

    public Optional<String> scanLabel(UUID scanId) {
        List<Object[]> rows = sql.query("SELECT s.target FROM scan s WHERE s.id = :id", Map.of("id", scanId));
        return rows.isEmpty() ? Optional.empty() : Optional.of(str(rows.get(0)[0]));
    }

    public Optional<String> policyLabel(Long policyId) {
        List<Object[]> rows = sql.query("SELECT p.name FROM policy p WHERE p.id = :id", Map.of("id", policyId));
        return rows.isEmpty() ? Optional.empty() : Optional.of(str(rows.get(0)[0]));
    }
}
