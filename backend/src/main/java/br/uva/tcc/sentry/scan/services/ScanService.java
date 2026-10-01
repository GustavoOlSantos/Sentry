package br.uva.tcc.sentry.scan.services;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.finding.checker.ConfigFindingService;
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

    private final ScanEngine scanEngine;
    private final VulnerabilityLookupService vulnerabilityLookupService;
    private final ConfigFindingService configFindingService;

    public ScanService(ScanEngine scanEngine,
                        VulnerabilityLookupService vulnerabilityLookupService,
                        ConfigFindingService configFindingService) {
        this.scanEngine = scanEngine;
        this.vulnerabilityLookupService = vulnerabilityLookupService;
        this.configFindingService = configFindingService;
    }

    public ArrayList<Scan> findAll() {
        return new ArrayList<>();
    }

    public Scan findById(UUID id) {
        return new Scan();
    }

    public ScanResponse executeScan(String target) {
        var scan = new Scan(target);

        ScanResult result = scanEngine.scan(target);

        for (DiscoveredHost host : result.getHosts()) {
            result.addConfigFindings(configFindingService.check(host));

            for (var port : host.services()) {
                if (port.hasCpe() && port.getVersion() != null) {
                    result.addVulnerabilities(vulnerabilityLookupService.lookupByCpe(port.getCpe()));
                }
            }
        }

        var response = new ScanResponse(scan, result);

        return response;
    }
}