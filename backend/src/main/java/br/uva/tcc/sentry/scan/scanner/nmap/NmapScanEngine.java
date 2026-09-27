package br.uva.tcc.sentry.scan.scanner.nmap;

import br.uva.tcc.sentry.finding.domain.Host;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;
import br.uva.tcc.sentry.scan.domain.ScanResult;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class NmapScanEngine implements ScanEngine {

    private final NmapCommandBuilder commandBuilder;
    private final NmapProcessExecutor processExecutor;
    private final NmapResultParser resultParser;

    public NmapScanEngine(NmapCommandBuilder commandBuilder, NmapProcessExecutor processExecutor, NmapResultParser resultParser) {
        this.commandBuilder = commandBuilder;
        this.processExecutor = processExecutor;
        this.resultParser = resultParser;
    }

    @Override
    public ScanResult scan(String target) {
        Path outputFile = createTemporaryOutputFile();

        try {
            List<String> command = commandBuilder.build(target, outputFile.toString());

            String xml = processExecutor.execute(command, outputFile);

            List<Host> hosts = resultParser.parse(xml);

            ScanResult result = new ScanResult(target, String.join(" ", command));
            hosts.forEach(result::addHost);
            return result;

        } finally {
            deleteTemporaryFile(outputFile);
        }
    }

    private Path createTemporaryOutputFile() {
        try {
            return Files.createTempFile("nmap-" + UUID.randomUUID(), ".xml");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create temporary Nmap output file", e);
        }
    }

    private void deleteTemporaryFile(Path outputFile) {
        try {
            Files.deleteIfExists(outputFile);
        } catch (Exception ignored) {
        }
    }
}