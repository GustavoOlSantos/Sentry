package br.uva.tcc.sentry.scan.services;

import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.finding.checker.ConfigFindingService;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.DTO.ScanResponse;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;
import br.uva.tcc.sentry.vulnerability.nvd.VulnerabilityLookupService;

/**
 * Orquestra o ciclo completo de um scan: dispara o {@link ScanEngine}
 * (hoje, Nmap) para descobrir hosts/portas, consulta o NVD por CVEs
 * conhecidas em cada porta identificada por CPE e roda as checagens de
 * configuração insegura em cada host — juntando tudo no {@link ScanResult}.
 */
@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final ScanEngine scanEngine;
    private final VulnerabilityLookupService vulnerabilityLookupService;
    private final ConfigFindingService configFindingService;
    private final FindingFactory findingFactory;

    public ScanService(ScanEngine scanEngine, VulnerabilityLookupService vulnerabilityLookupService, ConfigFindingService configFindingService, FindingFactory findingFactory) {
        this.scanEngine = scanEngine;
        this.vulnerabilityLookupService = vulnerabilityLookupService;
        this.configFindingService = configFindingService;
        this.findingFactory = findingFactory;
    }

    public ArrayList<Scan> findAll() {
        return new ArrayList<>();
    }

    public Scan findById(UUID id) {
        return new Scan();
    }

    public ScanResponse executeScan(String target) {
        log.info("Scan solicitado para o Host: " + target);

        var scan = new Scan(target);

        scan.ScanStarted();
        ScanResult result = scanEngine.scan(target);
        scan.setCommand(result.getCommand());

        for (DiscoveredHost host : result.getHosts()) {
            result.addFindings(configFindingService.check(host, scan));

            for (var service : host.services()) {

                if (service.hasCpe() && service.getVersion() != null) {
                    var vulnerabilities = vulnerabilityLookupService.lookupByCpe(service.getCpe());

                    for (var vulnerability : vulnerabilities) {
                        result.addFinding(findingFactory.fromVulnerability(host, service, vulnerability, scan));
                    }
                }
            }
        }

        var response = new ScanResponse(scan, result);
        scan.markFinished();
        log.info("Scan para o Host: " + target + " finalizado com sucesso.");

        return response;
    }
}