package br.uva.tcc.sentry.scan.messaging;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
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

        /* TODO: Finalizar o  Fluxo de ScanGroup quando a persistência estiver configurada
         - obter Scangroup pelo id no banco de dados
         - Verificar se esse é o primeiro scan do grupo a ser executado e mudar os status pra running
         - Seta o startedAt
         - Salvar no banco
         - Verifique se ainda existe algum scan do grupo em estado não terminal. Se não existir, finalize o grupo
        */

        Scan scan = new Scan(scanRequestReceived.scanId(), scanRequestReceived.target());
        scan.ScanStarted();

        try {
            log.info("Iniciando scan. ID: {}, Host: {}", scanRequestReceived.scanId(), scanRequestReceived.target());

            ScanResult result = scanEngine.scan(scan.getTarget());
            scan.setCommand(result.getCommand());

            if (result.getHosts().isEmpty()) {
                log.warn("Nenhum host encontrado. Scan ID: {}, Target: {}", scan.getId(), scan.getTarget());
            }

            for (DiscoveredHost host : result.getHosts()) {
                result.addFindings(configFindingService.check(host, scan));

                for (var service : host.services()) {
                    if (!service.hasCpe() || service.getVersion() == null) {
                        continue;
                    }

                    var vulnerabilities = vulnerabilityLookupService.lookupByCpe(service.getCpe());

                    for (var vulnerability : vulnerabilities) {
                        result.addFinding(findingFactory.fromVulnerability(host, service, vulnerability, scan));
                    }
                }
            }

            scan.markFinished();
            log.info("Scan finalizado com sucesso. ID: {}, Host: {}", scanRequestReceived.scanId(), scanRequestReceived.target());

        } catch (Exception e) {
            scan.markFailed();

            log.error("Falha ao executar scan. ID: {}, Host: {}", scanRequestReceived.scanId(), scanRequestReceived.target(), e);
        }
    }
}