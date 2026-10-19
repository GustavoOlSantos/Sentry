package br.uva.tcc.sentry.scan.messaging;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.exception.PermanentScanException;
import br.uva.tcc.sentry.scan.exception.TransientScanException;

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
    
    private static final Logger log = LoggerFactory.getLogger(ScanConsumer.class);

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
    
    @RabbitListener(queues = RabbitMQConfig.SCAN_QUEUE, containerFactory = "scanListenerContainerFactory")
    public void consumirMensagem(ScanMessage scanRequestReceived) {

        log.info("Iniciando scan. ID: {}, Host: {}", scanRequestReceived.scanId(), scanRequestReceived.target());

        try {
            processarMensagem(scanRequestReceived);
        } 
        catch (TransientScanException e) {
            log.warn("Falha transitória no scan {} para o alvo {}. O processamento será repetido.", scanRequestReceived.scanId(), scanRequestReceived.target(), e);
            throw e;
        } 
        catch (PermanentScanException e) {
            log.error("Falha permanente no scan {} para o alvo {}.", scanRequestReceived.scanId(), scanRequestReceived.target(), e);
            throw e;
        } 
        catch (Exception e) {
            log.error("Erro não classificado durante o scan {} para o alvo {}.", scanRequestReceived.scanId(), scanRequestReceived.target(), e);
            throw new PermanentScanException("Falha não classificada no processamento do scan", e);
        }
    }


     /* TODO (processarMensagem): Finalizar o  Fluxo de ScanGroup quando a persistência estiver configurada
         - obter Scangroup pelo id no banco de dados
         - Verificar se esse é o primeiro scan do grupo a ser executado e mudar os status pra running
         - Seta o startedAt
         - Salvar no banco
         - Verifique se ainda existe algum scan do grupo em estado não terminal. Se não existir, finalize o grupo
        */
    private void processarMensagem(ScanMessage scanRequestReceived){
        Scan scan = new Scan(scanRequestReceived.scanId(), scanRequestReceived.target());
        scan.ScanStarted();

        ScanResult result = scanEngine.scan(scan.getTarget());
        scan.setCommand(result.getCommand());

        if (result.getHosts().isEmpty()) {
            log.warn("Nenhum host encontrado. Scan ID: {}, Target: {}", scan.getId(), scan.getTarget());
            scan.addWarning("O Host não foi encontrado para o endereço informado");
            scan.markNotFounded();
            
            return;
        }

        for (DiscoveredHost host : result.getHosts()) {
            result.addFindings(configFindingService.check(host, scan));

            for (var service : host.services()) {
                if (!service.hasCpe() || service.getVersion() == null) {
                    continue;
                }

                var lookupResult = vulnerabilityLookupService.lookupByCpe(service.getCpe());

                if (!lookupResult.successful()) {
                    scan.addWarning(lookupResult.errorMessage());
                    continue;
                }

                for (var vulnerability : lookupResult.vulnerabilities()) {
                    result.addFinding(findingFactory.fromVulnerability(host, service, vulnerability, scan));
                }

                // TODO: Salvar as finding, services no Banco
                // TODO: Salvar cada vulnerability (Em finding) separada no banco de dados, checando se essa vulnerability já não existe no BD
            }
        }

        scan.markFinished();
        log.info("Scan finalizado com sucesso. ID: {}, Host: {}", scanRequestReceived.scanId(), scanRequestReceived.target());
    }
}