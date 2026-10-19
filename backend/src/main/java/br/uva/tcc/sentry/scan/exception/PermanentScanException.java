package br.uva.tcc.sentry.scan.exception;

public class PermanentScanException extends RuntimeException {

    public PermanentScanException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermanentScanException(String message) {
        super(message);
    }
}