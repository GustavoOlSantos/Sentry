package br.uva.tcc.sentry.finding.domain;

/** Quem causou uma mudança de status registrada em {@link FindingStatusHistory}. */
public enum HistoryOrigin {
    /** O processamento de um scan (detecção, reencontro, validação por ausência, reabertura). */
    SCAN,
    /** Uma pessoa, pelo Board. */
    USER,
    /**
     * Linha sintética criada para findings anteriores ao histórico, a partir dos timestamps
     * que já existiam (detectedAt e validatedAt). Não representa o que de fato aconteceu entre eles.
     */
    BACKFILL
}
