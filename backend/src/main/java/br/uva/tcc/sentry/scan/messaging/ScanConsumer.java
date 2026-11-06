package br.uva.tcc.sentry.scan.messaging;

import br.uva.tcc.sentry.config.RabbitMQConfig;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanAsset;
import br.uva.tcc.sentry.scan.domain.ScanCoverage;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;

import br.uva.tcc.sentry.scan.exception.PermanentScanException;
import br.uva.tcc.sentry.scan.exception.TransientScanException;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.service.AssetUpsertService;
import br.uva.tcc.sentry.finding.checker.ConfigFindingService;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.finding.service.FindingReconciler;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability;
import br.uva.tcc.sentry.vulnerability.repository.VulnerabilityRepository;
import br.uva.tcc.sentry.vulnerability.nvd.VulnerabilityLookupService;
import lombok.RequiredArgsConstructor;
import br.uva.tcc.sentry.scan.repository.ScanAssetRepository;
import br.uva.tcc.sentry.scan.repository.ScanExecutionGroupRepository;
import br.uva.tcc.sentry.scan.repository.ScanRepository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScanConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(ScanConsumer.class);

    private final ScanEngine scanEngine;
    private final VulnerabilityLookupService vulnerabilityLookupService;
    private final ConfigFindingService configFindingService;
    private final FindingFactory findingFactory;
    private final AssetUpsertService assetUpsertService;
    private final FindingReconciler findingReconciler;

    private final ScanRepository scanRepository;
    private final ScanExecutionGroupRepository scanExecutionGroupRepository;
    private final ScanAssetRepository scanAssetRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    
    
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

        // 1. identidade estável: reaproveita assets/serviços que já existem em vez de duplicá-los a cada scan
        ScanCoverage coverage = new ScanCoverage();
        List<DiscoveredHost> hosts = new ArrayList<>();

        for (DiscoveredHost discovered : result.getHosts()) {
            DiscoveredHost host = assetUpsertService.upsert(discovered);
            hosts.add(host);

            coverage.markHostScanned(host);
            registrarScanAsset(scan, host.asset());
        }
        result.setHosts(hosts);

        // 2. candidatos a finding: o que este scan observou
        for (DiscoveredHost host : hosts) {
            result.addFindings(configFindingService.check(host, scan));

            for (var service : host.services()) {
                if (!service.hasCpe() || service.getVersion() == null) {
                    // sem CPE/versão não dá para consultar o NVD: ausência de CVE aqui não prova nada
                    coverage.markServiceUnverified(host.asset(), service);
                    continue;
                }

                var lookupResult = vulnerabilityLookupService.lookupByCpe(service.getCpe());

                if (!lookupResult.successful()) {
                    scan.addWarning(lookupResult.errorMessage());
                    scanRepository.save(scan);
                    coverage.markServiceUnverified(host.asset(), service);
                    continue;
                }

                for (var vulnerability : lookupResult.vulnerabilities()) {
                    Vulnerability persistedVulnerability = obterOuSalvarVulnerability(vulnerability);
                    result.addFinding(findingFactory.fromVulnerability(host, service, persistedVulnerability, scan));
                }
            }
        }

        // 3. reconciliação: cria o que é novo, atualiza o que já existia e valida o que sumiu
        var reconciliation = reconciliarFindings(scan, result, coverage);
        log.info("Findings do scan {} reconciliados: {}", scan.getId(), reconciliation);

        scan.markFinished();
        scanRepository.save(scan);
        log.info("Scan finalizado com sucesso. ID: {}, Host: {}", scanRequestReceived.scanId(), scanRequestReceived.target());

        verificaGrupo(scanGroup);
    }

    /**
     * Conflitos de concorrência (outro scan do mesmo host reconciliando ao mesmo
     * tempo, ou o usuário mexendo no mesmo finding pelo Board) são passageiros:
     * viram {@link TransientScanException} para o scan ser tentado de novo, já
     * que a reconciliação é idempotente.
     */
    private FindingReconciler.Result reconciliarFindings(Scan scan, ScanResult result, ScanCoverage coverage) {
        try {
            return findingReconciler.reconcile(scan, result.getFindings(), coverage);
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
            throw new TransientScanException("Conflito de concorrência ao reconciliar os findings do scan " + scan.getId(), e);
        }
    }

    /** Idempotente: numa nova tentativa do mesmo scan, a associação scan/asset já pode existir. */
    private void registrarScanAsset(Scan scan, Asset asset) {
        if (!scanAssetRepository.existsByScanIdAndAssetId(scan.getId(), asset.getId())) {
            scanAssetRepository.save(new ScanAsset(scan, asset, asset.getLastSeenAt()));
        }
    }

    /**
     * Garante que a vulnerability exista no banco sem duplicar o CVE (cve_id é único).
     * Se já existir, reaproveita a registrada; se não, salva a nova.
     */
    private Vulnerability obterOuSalvarVulnerability(Vulnerability vulnerability) {
        return vulnerabilityRepository.findByCveId(vulnerability.getCveId())
                .orElseGet(() -> {
                    try {
                        return vulnerabilityRepository.save(vulnerability);
                    } catch (DataIntegrityViolationException e) {
                        // Outro scan salvou o mesmo CVE ao mesmo tempo
                        return vulnerabilityRepository.findByCveId(vulnerability.getCveId()).orElseThrow(() -> e);
                    }
                });
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