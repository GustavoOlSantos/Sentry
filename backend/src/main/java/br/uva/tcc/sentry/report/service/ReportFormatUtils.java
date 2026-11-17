package br.uva.tcc.sentry.report.service;

import java.util.ArrayList;
import java.util.List;

import br.uva.tcc.sentry.report.ReportFilter;
import br.uva.tcc.sentry.report.repository.ReportRepository;

final class ReportFormatUtils {

    private ReportFormatUtils() {
    }

    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static double percentage(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return round2((part * 100.0) / total);
    }

    static String formatDuration(Long seconds) {
        if (seconds == null) {
            return null;
        }
        long total = Math.max(0L, seconds);
        long days = total / 86400;
        long hours = (total % 86400) / 3600;
        long minutes = (total % 3600) / 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return minutes > 0 ? hours + "h " + minutes + "min" : hours + "h";
        }
        if (minutes > 0) {
            return minutes + "min";
        }
        return total + "s";
    }

    static String describeScope(ReportFilter filter, ReportRepository repository) {
        List<String> parts = new ArrayList<>();

        if (filter.assetId() != null) {
            String label = repository.assetLabel(filter.assetId()).orElse(filter.assetId().toString());
            parts.add("Ativo: " + label);
        }
        if (filter.scanId() != null) {
            String label = repository.scanLabel(filter.scanId()).orElse(filter.scanId().toString());
            parts.add("Scan: " + label);
        }
        if (filter.scanGroupId() != null) {
            parts.add("Grupo de scans: " + filter.scanGroupId().toString().substring(0, 8));
        }
        if (filter.policyId() != null) {
            String label = repository.policyLabel(filter.policyId()).orElse("#" + filter.policyId());
            parts.add("Política: " + label);
        }
        if (filter.severity() != null) {
            parts.add("Severidade: " + filter.severity().name());
        }
        if (filter.type() != null) {
            parts.add("Tipo: " + filter.type().name());
        }
        if (filter.status() != null) {
            parts.add("Status: " + filter.status().name());
        }

        return parts.isEmpty() ? "Todo o ambiente" : String.join(" | ", parts);
    }
}
