package br.uva.tcc.sentry.scan.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte exceções de validação de entrada e de execução do scan em
 * respostas HTTP apropriadas, em vez de deixar o Spring devolver um 500
 * genérico com stack trace pro cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedTargetException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedTarget(UnauthorizedTargetException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Alvos não autorizados ou possivelmente maliciosos", ex.getMessage());
    }

    @ExceptionHandler(TransientScanException.class)
    public ResponseEntity<Map<String, Object>> handleTransientScan(TransientScanException ex) {
        // falha passageira (timeout, interrupção) — faz sentido o cliente tentar de novo
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Falha temporária na execução do scan", ex.getMessage());
    }

    @ExceptionHandler(PermanentScanException.class)
    public ResponseEntity<Map<String, Object>> handlePermanentScan(PermanentScanException ex) {
        // falha que não vai se resolver sozinha tentando de novo (comando/alvo inválido,
        // nmap não instalado, XML malformado etc.)
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Falha na execução do scan", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        var body = Map.<String, Object>of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message
        );

        return ResponseEntity.status(status).body(body);
    }
}
