package br.uva.tcc.sentry.scan.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.exception.PermanentScanException;
import br.uva.tcc.sentry.scan.exception.TransientScanException;
import br.uva.tcc.sentry.scan.repository.ScanExecutionGroupRepository;
import br.uva.tcc.sentry.scan.repository.ScanRepository;

class ScanTerminationServiceTest {

    private ScanRepository scanRepository;
    private ScanExecutionGroupRepository groupRepository;
    private ScanTerminationService service;

    private ScanExecutionGroup group;
    private Scan scan;

    @BeforeEach
    void setUp() {
        scanRepository = mock(ScanRepository.class);
        groupRepository = mock(ScanExecutionGroupRepository.class);
        service = new ScanTerminationService(scanRepository, groupRepository);

        group = new ScanExecutionGroup();
        group.markStarted();
        scan = new Scan("192.168.0.10", group);
        scan.ScanStarted(); // RUNNING, como fica durante as tentativas

        when(scanRepository.findWithExecGroupById(scan.getId())).thenReturn(Optional.of(scan));
    }

    private static Message messageFor(UUID scanId) {
        String json = "{\"scanId\":\"" + scanId + "\",\"target\":\"192.168.0.10\",\"execGroupId\":\"" + UUID.randomUUID() + "\"}";
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void retriesExhausted_marksRunningScanAsFailed_andClosesTheGroup() {
        when(scanRepository.existsScansByGroupIdAndStatusIn(eq(group.getId()), anyCollection())).thenReturn(false);

        service.registrarFalhaDefinitiva(messageFor(scan.getId()),
                new RuntimeException("listener", new TransientScanException("O Nmap excedeu o tempo limite de 120s")));

        assertEquals(ScanStatus.FAILED, scan.getStatus());
        assertNotNull(scan.getFinishedAt());
        assertTrue(scan.getErrorMessage().startsWith("Tentativas esgotadas após falha temporária"));
        assertTrue(scan.getErrorMessage().contains("excedeu o tempo limite"));
        verify(scanRepository).save(scan);
        verify(groupRepository).save(group);
        assertEquals(ScanStatus.COMPLETED, group.getStatus()); // status gravado do grupo; o exibido é derivado
    }

    @Test
    void groupStaysOpen_whileAnotherScanIsPendingOrRunning() {
        when(scanRepository.existsScansByGroupIdAndStatusIn(eq(group.getId()), anyCollection())).thenReturn(true);

        service.registrarFalhaDefinitiva(messageFor(scan.getId()), new TransientScanException("timeout"));

        assertEquals(ScanStatus.FAILED, scan.getStatus());
        verify(groupRepository, never()).save(any());
        assertNull(group.getFinishedAt());
    }

    @Test
    void closeGroup_considersRunningScansAsActive() {
        when(scanRepository.existsScansByGroupIdAndStatusIn(eq(group.getId()), anyCollection())).thenReturn(true);

        service.closeGroupIfFinished(group);

        verify(scanRepository).existsScansByGroupIdAndStatusIn(group.getId(),
                java.util.List.of(ScanStatus.PENDING, ScanStatus.RUNNING));
        verify(groupRepository, never()).save(any());
    }

    @Test
    void scanAlreadyTerminal_isLeftUntouched() {
        scan.setErrorMessage("erro original");
        scan.markFailed();

        boolean changed = service.markFailed(scan.getId(), "outro motivo");

        assertFalse(changed);
        assertEquals("erro original", scan.getErrorMessage());
        verify(scanRepository, never()).save(any());
    }

    @Test
    void unknownScan_isIgnored() {
        UUID unknown = UUID.randomUUID();
        when(scanRepository.findWithExecGroupById(unknown)).thenReturn(Optional.empty());

        assertFalse(service.markFailed(unknown, "x"));
        verify(scanRepository, never()).save(any());
    }

    @Test
    void messageWithoutReadableScanId_changesNothing() {
        Message garbage = new Message("não é json".getBytes(StandardCharsets.UTF_8), new MessageProperties());

        service.registrarFalhaDefinitiva(garbage, new TransientScanException("timeout"));

        assertEquals(ScanStatus.RUNNING, scan.getStatus());
        verify(scanRepository, never()).save(any());
    }

    @Test
    void hugeNmapOutput_isTruncatedToTheColumnSize() {
        String huge = "x".repeat(8_000);

        service.markFailed(scan.getId(), huge);

        assertEquals(ScanTerminationService.MAX_ERROR_LENGTH, scan.getErrorMessage().length());
        assertTrue(scan.getErrorMessage().endsWith("(truncado)"));
    }

    @Test
    void describe_permanentFailure_hasNoRetryPrefix() {
        String text = ScanTerminationService.describe(new RuntimeException("wrap", new PermanentScanException("comando inválido")));

        assertEquals("comando inválido", text);
    }

    @Test
    void extractScanId_readsTheIdFromTheJsonBody() {
        UUID id = UUID.randomUUID();

        assertEquals(Optional.of(id), ScanTerminationService.extractScanId(messageFor(id)));
    }
}
