package br.uva.tcc.sentry.scan.messaging;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.services.ScanService;

import br.uva.tcc.sentry.finding.checker.ConfigFindingService;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;
import br.uva.tcc.sentry.vulnerability.nvd.VulnerabilityLookupService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.uva.tcc.sentry.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ScanConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final ScanEngine scanEngine;
    private final VulnerabilityLookupService vulnerabilityLookupService;
    private final ConfigFindingService configFindingService;
    private final FindingFactory findingFactory;
    
    public ScanConsumer(ScanEngine scanEngine, VulnerabilityLookupService vulnerabilityLookupService, ConfigFindingService configFindingService, FindingFactory findingFactory) {
        this.scanEngine = scanEngine;
        this.vulnerabilityLookupService = vulnerabilityLookupService;
        this.configFindingService = configFindingService;
        this.findingFactory = findingFactory;
    }
    
    @RabbitListener(queues = RabbitMQConfig.SCAN_QUEUE)
    public void consumirMensagem(ScanMessage scanRequestReceived) {

        log.info("================================================");
        log.info("Host: " + scanRequestReceived.target());
        log.info("Scan ID: " + scanRequestReceived.scanId());
        log.info("================================================");

        Scan scan = new Scan(scanRequestReceived.scanId(), scanRequestReceived.target());
        scan.ScanStarted();
        scan.setStatus(ScanStatus.RUNNING);

        ScanResult result = scanEngine.scan(scan.getTarget());
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

        scan.markFinished();

        log.info("================================================");
        log.info("Scan Finalizado com sucesso!");
        log.info("Host: " + scanRequestReceived.target());
        log.info("================================================");
    }
}
