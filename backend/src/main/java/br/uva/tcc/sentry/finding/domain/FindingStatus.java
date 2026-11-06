package br.uva.tcc.sentry.finding.domain;

import java.util.EnumSet;
import java.util.Set;

public enum FindingStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    VALIDATED,
    JUSTIFIED,
    FAILED_VALIDATION;

    /** Status que o usuário pode atribuir manualmente (os demais são exclusivos da ferramenta). */
    private static final Set<FindingStatus> USER_SETTABLE = EnumSet.of(OPEN, IN_PROGRESS, RESOLVED, JUSTIFIED);

    /**
     * Status em que o finding ainda está "em jogo" para a reconciliação: se ele
     * deixar de aparecer nos scans (com cobertura), pode ser validado. VALIDATED
     * já está fechado e JUSTIFIED é um risco aceito, então ficam de fora.
     */
    private static final Set<FindingStatus> RECONCILABLE = EnumSet.of(OPEN, IN_PROGRESS, RESOLVED, FAILED_VALIDATION);

    public boolean isUserSettable() {
        return USER_SETTABLE.contains(this);
    }

    public static Set<FindingStatus> reconcilable() {
        return RECONCILABLE;
    }
}
