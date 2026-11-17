package br.uva.tcc.sentry.report;

import java.util.List;

public enum ReportSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO;

    public static final List<ReportSeverity> ORDERED = List.of(CRITICAL, HIGH, MEDIUM, LOW, INFO);

    public static ReportSeverity of(String dbValue) {
        if (dbValue == null) {
            return INFO;
        }
        return switch (dbValue) {
            case "CRITICAL" -> CRITICAL;
            case "HIGH" -> HIGH;
            case "MEDIUM" -> MEDIUM;
            case "LOW" -> LOW;
            default -> INFO;
        };
    }
}
