package br.uva.tcc.sentry.scan.services;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.exception.PermanentScanException;
import br.uva.tcc.sentry.scan.exception.TransientScanException;
import br.uva.tcc.sentry.scan.repository.ScanExecutionGroupRepository;
import br.uva.tcc.sentry.scan.repository.ScanRepository;
import lombok.RequiredArgsConstructor;

/**
 * Encerramento de scans que não vão mais terminar com sucesso, e do grupo a que pertencem.
 *
 * Existe porque o scan fica RUNNING desde que o consumer o pega. Quando o retry se esgota (ou a
 * falha é definitiva), a mensagem vai para a DLQ e ninguém mais toca no scan: sem este serviço
 * ele ficaria RUNNING para sempre, e o grupo junto.
 */
@Service
@RequiredArgsConstructor
public class ScanTerminationService {

    private static final Logger log = LoggerFactory.getLogger(ScanTerminationService.class);

    /** Tamanho da coluna {@code scan.error_message}; texto maior estoura no banco e o scan não seria atualizado. */
    static final int MAX_ERROR_LENGTH = 2000;

    /** Estados em que o grupo ainda tem trabalho a fazer. */
    private static final List<ScanStatus> ACTIVE_STATUSES = List.of(ScanStatus.PENDING, ScanStatus.RUNNING);

    private static final Pattern SCAN_ID_IN_BODY =
            Pattern.compile("\"scanId\"\\s*:\\s*\"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\"");

    private final ScanRepository scanRepository;
    private final ScanExecutionGroupRepository scanExecutionGroupRepository;

    /**
     * Chamado quando a mensagem está indo para a DLQ (tentativas esgotadas ou falha definitiva).
     * Se o scan ainda não estiver em estado terminal, vira FAILED e o grupo é encerrado se for o caso.
     */
    @Transactional
    public void registrarFalhaDefinitiva(Message message, Throwable cause) {
        Optional<UUID> scanId = extractScanId(message);

        if (scanId.isEmpty()) {
            log.error("Mensagem enviada à DLQ sem um scanId legível; nenhum scan pôde ser marcado como FAILED.");
            return;
        }

        markFailed(scanId.get(), describe(cause));
    }

    /**
     * Marca o scan como FAILED e encerra o grupo se não sobrou scan ativo.
     * Idempotente: scan inexistente ou já em estado terminal não é alterado.
     *
     * @return true se o scan foi marcado agora como FAILED
     */
    @Transactional
    public boolean markFailed(UUID scanId, String reason) {
        Optional<Scan> found = scanRepository.findWithExecGroupById(scanId);

        if (found.isEmpty()) {
            log.warn("Scan {} não encontrado ao tentar marcá-lo como FAILED.", scanId);
            return false;
        }

        Scan scan = found.get();

        if (isTerminal(scan.getStatus())) {
            log.info("O scan {} já está em um estado terminal ({}); nada a fazer.", scanId, scan.getStatus());
            return false;
        }

        scan.setErrorMessage(truncate(reason));
        scan.markFailed();
        scanRepository.save(scan);

        log.error("Scan {} finalizado com falha. Motivo: {}", scanId, reason);

        if (scan.getExecGroup() != null) {
            closeGroupIfFinished(scan.getExecGroup());
        }
        return true;
    }

    /** O grupo só termina quando nenhum scan dele está PENDING nem RUNNING. */
    @Transactional
    public void closeGroupIfFinished(ScanExecutionGroup group) {
        boolean hasActiveScans = scanRepository.existsScansByGroupIdAndStatusIn(group.getId(), ACTIVE_STATUSES);

        if (!hasActiveScans) {
            group.markFinished();
            scanExecutionGroupRepository.save(group);
        }
    }

    // ---------------- auxiliares (package-private para teste) ----------------

    static boolean isTerminal(ScanStatus status) {
        return status == ScanStatus.COMPLETED
                || status == ScanStatus.NOT_FOUND
                || status == ScanStatus.FAILED
                || status == ScanStatus.CANCELLED;
    }

    /** Lê o scanId direto do JSON da mensagem (o conversor do Spring depende do tipo inferido pelo listener). */
    static Optional<UUID> extractScanId(Message message) {
        if (message == null || message.getBody() == null) {
            return Optional.empty();
        }

        Matcher matcher = SCAN_ID_IN_BODY.matcher(new String(message.getBody(), StandardCharsets.UTF_8));
        return matcher.find() ? Optional.of(UUID.fromString(matcher.group(1))) : Optional.empty();
    }

    /** Texto gravado em {@code errorMessage}: a causa de negócio, com aviso se foi o retry que se esgotou. */
    static String describe(Throwable cause) {
        Throwable business = null;
        Throwable deepest = cause;
        boolean transientFailure = false;

        Throwable current = cause;
        for (int depth = 0; current != null && depth < 20; depth++) {
            if (current instanceof TransientScanException) {
                transientFailure = true;
                business = business != null ? business : current;
            } else if (current instanceof PermanentScanException) {
                business = business != null ? business : current;
            }
            deepest = current;
            current = current.getCause();
        }

        Throwable chosen = business != null ? business : deepest;
        String reason = chosen == null || chosen.getMessage() == null
                ? (chosen == null ? "causa desconhecida" : chosen.getClass().getSimpleName())
                : chosen.getMessage();

        return transientFailure ? "Tentativas esgotadas após falha temporária: " + reason : reason;
    }

    static String truncate(String text) {
        if (text == null) {
            return null;
        }
        String suffix = "... (truncado)";
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH - suffix.length()) + suffix;
    }
}
