package br.uva.tcc.sentry.scan.domain;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
class ScanLifecycleTest {
 @Test void newScanStartsAsPending(){Scan scan=new Scan("192.168.1.10");assertEquals(ScanStatus.PENDING,scan.getStatus());assertNotNull(scan.getId());assertNotNull(scan.getCreatedAt());assertEquals("192.168.1.10",scan.getTarget());}
 @Test void startingScanMovesItToRunning(){Scan scan=new Scan("192.168.1.10");scan.ScanStarted();assertEquals(ScanStatus.RUNNING,scan.getStatus());assertNotNull(scan.getStartedAt());}
 @Test void terminalOperationsSetExpectedStatusAndFinishTime(){Scan completed=new Scan("10.0.0.1");completed.markFinished();assertEquals(ScanStatus.COMPLETED,completed.getStatus());assertNotNull(completed.getFinishedAt());Scan failed=new Scan("10.0.0.2");failed.markFailed();assertEquals(ScanStatus.FAILED,failed.getStatus());assertNotNull(failed.getFinishedAt());Scan notFound=new Scan("10.0.0.3");notFound.markNotFounded();assertEquals(ScanStatus.NOT_FOUND,notFound.getStatus());Scan cancelled=new Scan("10.0.0.4");cancelled.markCanceled();assertEquals(ScanStatus.CANCELLED,cancelled.getStatus());}
 @Test void addWarningIgnoresNullAndBlankValues(){Scan scan=new Scan("10.0.0.1");scan.addWarning(null);scan.addWarning("");scan.addWarning("   ");scan.addWarning("OS detection limitada");assertEquals(1,scan.getWarnings().size());assertEquals("OS detection limitada",scan.getWarnings().get(0));}
}
