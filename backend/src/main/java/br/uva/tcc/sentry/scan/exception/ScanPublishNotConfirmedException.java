package br.uva.tcc.sentry.scan.exception;

import org.springframework.amqp.AmqpException;

public class ScanPublishNotConfirmedException extends AmqpException {

    public ScanPublishNotConfirmedException(String message) {
        super(message);
    }

    public ScanPublishNotConfirmedException(String message, Throwable cause) {
        super(message, cause);
    }
}
