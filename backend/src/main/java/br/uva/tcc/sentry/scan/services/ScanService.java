package br.uva.tcc.sentry.scan.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.finding.checker.ConfigFindingService;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.DTO.ScanResponse;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.exception.UnauthorizedTargetException;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;
import br.uva.tcc.sentry.scan.validation.TargetValidator;
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
    private final TargetValidator targetValidator;

    public ScanService(ScanEngine scanEngine, VulnerabilityLookupService vulnerabilityLookupService,
            ConfigFindingService configFindingService, FindingFactory findingFactory,
            TargetValidator targetValidator) {
        this.scanEngine = scanEngine;
        this.vulnerabilityLookupService = vulnerabilityLookupService;
        this.configFindingService = configFindingService;
        this.findingFactory = findingFactory;
        this.targetValidator = targetValidator;
    }

    public ArrayList<Scan> findAll() {
        return new ArrayList<>();
    }

    public Scan findById(UUID id) {
        return new Scan();
    }

    public ArrayList<ScanResponse> executeScan(String strTargets) {
        var response = new  ArrayList<ScanResponse>();

        var targets = targetsToList(strTargets);
        log.info("Scan solicitado para os  Hosts: " + targets);

        for (String target : targets) {

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

            response.add(new ScanResponse(scan, result));
            scan.markFinished();
            log.info("Scan para o Host: " + target + " finalizado com sucesso.");
        }

        return response;
    }

    /**
     * Converte a string recebida em {@code targets} numa lista de hosts,
     * rejeitando qualquer entrada que não seja um IPv4, IPv6 ou range CIDR
     * válido. Isso impede que alguém use esse campo para injetar flags ou
     * argumentos extras no comando do Nmap (ex.: "127.0.0.1 --script=...").
     */
    public List<String> targetsToList(String targets) {
        targets = targets.replaceAll("\\s+", "");
        List<String> list = Arrays.asList(targets.split(","));

        List<String> invalid = list.stream()
                .filter(target -> !targetValidator.isValid(target))
                .collect(Collectors.toList());

        if (!invalid.isEmpty()) {
            log.warn("Tentativa de scan com targets inválidos: " + invalid);
            throw new UnauthorizedTargetException(
                    "Entrada inválida em 'targets': " + String.join(", ", invalid)
                            + ". Apenas IPv4, IPv6 ou notação CIDR (IP/prefixo) são aceitos.");
        }

        return list;
    }
}
