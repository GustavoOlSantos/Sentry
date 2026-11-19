package br.uva.tcc.sentry.scan.scanner.nmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.uva.tcc.sentry.scan.exception.PermanentScanException;

class NmapCommandBuilderTest {

    private final NmapCommandBuilder builder = new NmapCommandBuilder();

    @Test
    void buildsExpectedNmapCommandWithFullTcpPortRange() {
        assertEquals(
                List.of("nmap", "-sT", "-p-", "-sV", "-O", "-oX", "/tmp/scan.xml", "192.168.1.10"),
                builder.build("192.168.1.10", "/tmp/scan.xml"));
    }

    @Test
    void scansAllTcpPortsInsteadOfOnlyTheDefaultTopThousand() {
        List<String> command = builder.build("192.168.1.10", "/tmp/scan.xml");

        assertTrue(command.contains("-p-"));
        assertTrue(command.contains("-sT"));
    }

    @Test
    void keepsTargetAsSingleCommandArgument() {
        List<String> command = builder.build("2001:db8::1", "result.xml");

        assertEquals("2001:db8::1", command.get(command.size() - 1));
    }

    @Test
    void rejectsBlankTarget() {
        assertThrows(PermanentScanException.class, () -> builder.build(null, "result.xml"));
        assertThrows(PermanentScanException.class, () -> builder.build("   ", "result.xml"));
    }
}
