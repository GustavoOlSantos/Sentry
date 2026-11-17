package br.uva.tcc.sentry.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.exception.InvalidReportFilterException;
import br.uva.tcc.sentry.shared.domain.Severity;

public record ReportFilter(
        LocalDate from,
        LocalDate to,
        UUID assetId,
        UUID scanId,
        UUID scanGroupId,
        Severity severity,
        FindingType type,
        FindingStatus status,
        Long policyId) {

    public static final int DEFAULT_DAYS = 30;
    public static final int MAX_DAYS = 366;

    public static ReportFilter of(LocalDate from, LocalDate to, UUID assetId, UUID scanId, UUID scanGroupId,
                                  Severity severity, FindingType type, FindingStatus status, Long policyId,
                                  LocalDate today) {
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);

        if (start.isAfter(end)) {
            throw new InvalidReportFilterException("A data inicial deve ser anterior ou igual à data final.");
        }

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_DAYS) {
            throw new InvalidReportFilterException(
                    "O período máximo é de " + MAX_DAYS + " dias (informado: " + days + " dias).");
        }

        return new ReportFilter(start, end, assetId, scanId, scanGroupId, severity, type, status, policyId);
    }

    public LocalDateTime fromStart() {
        return from.atStartOfDay();
    }

    public LocalDateTime toExclusive() {
        return to.plusDays(1).atStartOfDay();
    }

    public int days() {
        return (int) (ChronoUnit.DAYS.between(from, to) + 1);
    }
}
