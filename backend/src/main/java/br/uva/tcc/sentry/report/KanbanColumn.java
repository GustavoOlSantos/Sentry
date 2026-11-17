package br.uva.tcc.sentry.report;

import java.util.List;

import br.uva.tcc.sentry.finding.domain.FindingStatus;

public enum KanbanColumn {
    ABERTO,
    EM_ANDAMENTO,
    RESOLVIDA,
    FECHADA;

    public static final List<KanbanColumn> ORDERED = List.of(ABERTO, EM_ANDAMENTO, RESOLVIDA, FECHADA);

    public static KanbanColumn of(FindingStatus status) {
        return switch (status) {
            case OPEN -> ABERTO;
            case IN_PROGRESS, FAILED_VALIDATION -> EM_ANDAMENTO;
            case RESOLVED -> RESOLVIDA;
            case JUSTIFIED, VALIDATED -> FECHADA;
        };
    }
}
