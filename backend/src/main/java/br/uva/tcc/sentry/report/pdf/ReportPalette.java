package br.uva.tcc.sentry.report.pdf;

import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.report.ReportSeverity;

final class ReportPalette {

    static final int NAVY = 0x0F172A;
    static final int BLUE = 0x1F6FEB;
    static final int BLUE_LIGHT = 0xDCE9FD;
    static final int PURPLE = 0x8B5CF6;
    static final int PURPLE_LIGHT = 0xEDE7FD;

    static final int TEXT = 0x111827;
    static final int TEXT_MUTED = 0x6B7280;
    static final int BORDER = 0xD9DEE7;
    static final int ZEBRA = 0xF5F7FB;
    static final int GRID = 0xE5E9F0;
    static final int WHITE = 0xFFFFFF;

    static final int GREEN = 0x2FA84F;
    static final int GRAY = 0x9CA3AF;

    private ReportPalette() {
    }

    static int severity(ReportSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0xD92D4A;
            case HIGH -> 0xF07A22;
            case MEDIUM -> 0xE0A800;
            case LOW -> 0x2FA84F;
            case INFO -> 0x4C8DF6;
        };
    }

    static int status(FindingStatus status) {
        return switch (status) {
            case OPEN -> 0xD92D4A;
            case IN_PROGRESS -> 0xE0A800;
            case FAILED_VALIDATION -> 0xF07A22;
            case RESOLVED -> 0x4C8DF6;
            case JUSTIFIED -> PURPLE;
            case VALIDATED -> 0x2FA84F;
        };
    }
}
