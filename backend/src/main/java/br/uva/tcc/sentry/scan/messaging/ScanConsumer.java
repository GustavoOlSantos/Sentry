package br.uva.tcc.sentry.scan.messaging;

import br.uva.tcc.sentry.config.RabbitMQConfig;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanAsset;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;

import br.uva.tcc.sentry.scan.exception.PermanentScanException;
import br.uva.tcc.sentry.scan.exception.TransientScanException;

import br.uva.tcc.sentry.finding.checker.ConfigFindingService;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.vulnerability.nvd.VulnerabilityLookupService;
import br.uva.tcc.sentry.scan.repository.ScanAssetRepository;
import br.uva.tcc.sentry.scan.repository.ScanExecutionGroupRepository;
import br.uva.tcc.sentry.scan.repository.ScanRepository;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.Asset.repository.AssetRepository;
import br.uva.tcc.sentry.Asset.repository.ServiceRepository;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ScanConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(ScanConsumer.class);

    private final ScanEngine scanEngine;
    private final VulnerabilityLookupService vulnerabilityLookupService;
    private final ConfigFindingService configFindingService;
    private final FindingFactory findingFactory;

    private final ScanRepository scanRepository;
    private final AssetRepository assetRepository;
    private final ServiceRepository serviceRepository;
    private final ScanExecutionGroupRepository scanExecutionGroupRepository;
    private final ScanAssetRepository scanAssetRepository;
    
    public ScanConsumer(ScanEngine scanEngine, VulnerabilityLookupService vulnerabilityLookupService, ConfigFindingService configFindingService, FindingFactory findingFactory, ScanRepository scanRepository, ScanExecutionGroupRepository scanExecutionGroupRepository, AssetRepository assetRepository, ServiceRepository serviceRepository, ScanAssetRepository scanAssetRepository) {
        this.scanEngine = scanEngine;
        this.vulnerabilityLookupService = vulnerabilityLookupService;
        this.configFindingService = configFindingService;
        this.findingFactory = findingFactory;
        this.scanRepository = scanRepository;
        this.assetRepository = assetRepository;
        this.serviceRepository = serviceRepository;
        this.scanExecutionGroupRepository = scanExecutionGroupRepository;
        this.scanAssetRepository = scanAssetRepository;
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
            finalizarScanComFalha(scanRequestReceived, e);
            throw e;
        } 
        catch (Exception e) {
            log.error("Erro não classificado durante o scan {} para o alvo {}.", scanRequestReceived.scanId(), scanRequestReceived.target(), e);
            finalizarScanComFalha(scanRequestReceived, e);
            throw new PermanentScanException("Falha não classificada no processamento do scan", e);
        }
    }

    private void processarMensagem(ScanMessage scanRequestReceived){

        Scan scan = scanRepository.findWithExecGroupById(scanRequestReceived.scanId()).orElseThrow(() -> new PermanentScanException("Scan não encontrado: " + scanRequestReceived.scanId()));
        ScanExecutionGroup scanGroup = scan.getExecGroup();

        if(scanGroup.getStatus() == ScanStatus.PENDING){
            scanGroup.markStarted();
            scanExecutionGroupRepository.save(scanGroup);
        }

        scan.ScanStarted();
        scanRepository.save(scan);

        ScanResult result = scanEngine.scan(scan.getTarget());
        scan.setCommand(result.getCommand());

        if (result.getHosts().isEmpty()) {
            log.warn("Nenhum host encontrado. Scan ID: {}, Target: {}", scan.getId(), scan.getTarget());
            scan.addWarning("O Host não foi encontrado para o endereço informado");
            scan.markNotFounded();
            scanRepository.save(scan);
            
            verificaGrupo(scanGroup);
            return;
        }

        for (DiscoveredHost host : result.getHosts()) {
            assetRepository.save(host.asset());

            var scanAsset = new ScanAsset(scan, host.asset(), host.asset().getFirstSeenAt());
            scanAssetRepository.save(scanAsset);

            for(Service service : host.services()){
                serviceRepository.save(service);
            }
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
                    scanRepository.save(scan);
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
        scanRepository.save(scan);
        log.info("Scan finalizado com sucesso. ID: {}, Host: {}", scanRequestReceived.scanId(), scanRequestReceived.target());

        verificaGrupo(scanGroup);
    }

    public void verificaGrupo(ScanExecutionGroup scanGroup){
        boolean hasActiveScans = scanRepository.existsScansByGroupIdAndStatusIn(
            scanGroup.getId(),
            List.of(ScanStatus.PENDING)
        );

        if (!hasActiveScans) {
            scanGroup.markFinished();
            scanExecutionGroupRepository.save(scanGroup);
        }
    }

    private void finalizarScanComFalha(ScanMessage scanRequestReceived, Exception exception) {

    Scan scan = scanRepository.findWithExecGroupById(scanRequestReceived.scanId()).orElseThrow(() -> new PermanentScanException("Scan não encontrado: " + scanRequestReceived.scanId()));

    if (scan.getStatus() == ScanStatus.COMPLETED
            || scan.getStatus() == ScanStatus.NOT_FOUND
            || scan.getStatus() == ScanStatus.FAILED) {
        log.warn("O scan {} já está em um estado terminal: {}.", scan.getId(), scan.getStatus());
        return;
    }

    scan.setErrorMessage(exception.getMessage());
    scan.markFailed();
    scanRepository.save(scan);

    log.error("Scan {} finalizado com falha. Motivo: {}", scan.getId(), exception.getMessage());

    verificaGrupo(scan.getExecGroup());
}
}