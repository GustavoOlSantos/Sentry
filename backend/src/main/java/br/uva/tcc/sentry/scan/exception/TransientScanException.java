package br.uva.tcc.sentry.scan.exception;

public class TransientScanException extends RuntimeException {

    public TransientScanException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransientScanException(String message) {
        super(message);
    }
}