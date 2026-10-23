package br.uva.tcc.sentry.scan.scanner.nmap;

import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;
import br.uva.tcc.sentry.scan.domain.ScanResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class NmapScanEngine implements ScanEngine {

    private static final Logger log = LoggerFactory.getLogger(NmapScanEngine.class);

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

            List<DiscoveredHost> hosts = resultParser.parse(xml);

            ScanResult result = new ScanResult(String.join(" ", command));
            hosts.forEach(result::addHost);
            return result;

        } finally {
            // roda sempre — em sucesso, erro ou timeout (o processExecutor já
            // garante que o processo foi encerrado antes de chegar aqui)
            deleteTemporaryFile(outputFile);
        }
    }

    private Path createTemporaryOutputFile() {
        try {
            return Files.createTempFile("nmap-" + UUID.randomUUID(), ".xml");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar o arquivo temporário de saída do Nmap", e);
        }
    }

    private void deleteTemporaryFile(Path outputFile) {
        try {
            boolean deleted = Files.deleteIfExists(outputFile);
            if (!deleted) {
                log.debug("Arquivo temporário já não existia ao tentar limpar: {}", outputFile);
            }
        } catch (Exception e) {
            // não é motivo pra derrubar o scan, mas precisa ficar registrado —
            // arquivo temporário órfão acumulando no disco é um problema real
            log.warn("Não foi possível apagar o arquivo temporário do Nmap: {}", outputFile, e);
        }
    }
}
