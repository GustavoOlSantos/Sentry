package br.uva.tcc.sentry.scan.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte exceções de validação de entrada em respostas HTTP apropriadas,
 * em vez de deixar o Spring devolver um 500 genérico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedTargetException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedTarget(UnauthorizedTargetException ex) {
        var body = Map.<String, Object>of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Alvos não autorizados ou possivelmente maliciosos",
                "message", ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
