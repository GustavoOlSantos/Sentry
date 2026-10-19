package br.uva.tcc.sentry.scan.exception;

import org.springframework.amqp.AmqpException;

public class ScanPublishFailedException extends AmqpException {

    public ScanPublishFailedException(String message) {
        super(message);
    }

    public ScanPublishFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

