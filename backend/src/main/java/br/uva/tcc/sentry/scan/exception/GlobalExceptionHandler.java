package br.uva.tcc.sentry.scan.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.uva.tcc.sentry.auth.exception.EmailAlreadyInUseException;
import br.uva.tcc.sentry.auth.exception.InvalidCredentialsException;
import br.uva.tcc.sentry.auth.exception.InvalidPasswordException;
import br.uva.tcc.sentry.auth.exception.LastAdminException;
import br.uva.tcc.sentry.finding.exception.InvalidFindingTransitionException;
import jakarta.persistence.EntityNotFoundException;

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

    @SuppressWarnings("deprecation")
    @ExceptionHandler(PermanentScanException.class)
    public ResponseEntity<Map<String, Object>> handlePermanentScan(PermanentScanException ex) {
        // falha que não vai se resolver sozinha tentando de novo (comando/alvo inválido,
        // nmap não instalado, XML malformado etc.)
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Falha na execução do scan", ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso não encontrado", String.valueOf(ex.getMessage()));
    }

    @ExceptionHandler(InvalidFindingTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFindingTransition(InvalidFindingTransitionException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Mudança de status não permitida", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        // o finding foi alterado (ex.: por um scan) entre a leitura e a gravação
        return buildResponse(HttpStatus.CONFLICT, "Conflito de edição",
                "O finding foi alterado por outro processo. Recarregue o board e tente novamente.");
    }

    // ---------------- autenticação e usuários ----------------

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
        return buildResponse(HttpStatus.CONFLICT, "E-mail já cadastrado", ex.getMessage());
    }

    @ExceptionHandler(LastAdminException.class)
    public ResponseEntity<Map<String, Object>> handleLastAdmin(LastAdminException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Operação não permitida", ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPassword(InvalidPasswordException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Senha inválida", ex.getMessage());
    }

    // ---------------- validação de entrada ----------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, "Dados inválidos",
                details.isBlank() ? "Os dados enviados são inválidos." : details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // corpo ausente, JSON malformado ou valor de enum inexistente (ex.: role = "ROOT")
        return buildResponse(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "O corpo da requisição está ausente ou malformado.");
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
