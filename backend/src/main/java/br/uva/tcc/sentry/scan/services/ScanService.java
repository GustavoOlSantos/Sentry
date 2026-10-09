package br.uva.tcc.sentry.scan.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.scan.messaging.ScanMessage;
import br.uva.tcc.sentry.scan.messaging.ScanProducer;
import br.uva.tcc.sentry.finding.checker.ConfigFindingService;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.scan.DTO.ScanResponse;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
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
    private final ScanProducer scanProducer;
    
    public ScanService(ScanProducer scanProducer) {
        this.scanProducer = scanProducer;
    }

    public ArrayList<Scan> findAll() {
        return new ArrayList<>();
    }

    public Scan findById(UUID id) {
        return new Scan();
    }

    public ScanResponse executeScan(String target) {
        Map<String, UUID> scanningHosts = new HashMap<>();
        List<String> failedTargets = new ArrayList<>();
        
        var scan = new Scan(target);

        log.info("Recebendo solicitação para scan do Host: " + target);

        scanProducer.enviarMensagem(new ScanMessage(scan.getId(), target));

        scanningHosts.put(scan.getTarget(), scan.getId());

        ScanResponse response = new ScanResponse(
            "Solicitações de scan aceitas para processamento.", 
            ScanStatus.PENDING, 
            scanningHosts.size(), 
            scanningHosts, 
            failedTargets);

        return response;
    }
}