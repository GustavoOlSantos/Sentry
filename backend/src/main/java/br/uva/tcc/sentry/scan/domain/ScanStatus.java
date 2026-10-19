package br.uva.tcc.sentry.scan.domain;

public enum ScanStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    NOT_FOUND,
    CANCELLED,
    /* Scan GroupException */
    ACCEPTED,
    PARTIAL,
}

