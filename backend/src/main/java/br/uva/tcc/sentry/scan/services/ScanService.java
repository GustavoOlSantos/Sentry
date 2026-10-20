package br.uva.tcc.sentry.scan.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.scan.messaging.ScanMessage;
import br.uva.tcc.sentry.scan.messaging.ScanProducer;
import br.uva.tcc.sentry.scan.messaging.ScanPublishResult;
import br.uva.tcc.sentry.scan.messaging.ScanPublishStatus;
import br.uva.tcc.sentry.scan.DTO.ScanResponse;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.repository.ScanRepository;
import br.uva.tcc.sentry.scan.repository.ScanExecutionGroupRepository;

import br.uva.tcc.sentry.scan.exception.ScanPublishNotConfirmedException;
import br.uva.tcc.sentry.scan.exception.UnauthorizedTargetException;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;
import br.uva.tcc.sentry.scan.validation.TargetValidator;

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
    private final TargetValidator targetValidator;
    private final ScanRepository scanRepository;
    private final ScanExecutionGroupRepository scanExecutionGroupRepository;

    public ScanService(ScanProducer scanProducer, TargetValidator targetValidator, ScanRepository scanRepository, ScanExecutionGroupRepository scanExecutionGroupRepository) {
        this.scanProducer = scanProducer;
        this.targetValidator = targetValidator;
        this.scanRepository = scanRepository;
        this.scanExecutionGroupRepository = scanExecutionGroupRepository;
    }

    public ArrayList<Scan> findAll() {
        return new ArrayList<>();
    }

    public Scan findById(UUID id) {
        return new Scan();
    }

   
    public ScanResponse executeScan(String strTargets) {
        var targets = targetsToList(strTargets);
        var scanGroup = new ScanExecutionGroup();
        scanExecutionGroupRepository.save(scanGroup);

        List<ScanPublishResult> publishResults = new ArrayList<>();

        log.info("Recebendo solicitação para scan dos Hosts: {}", targets);

        for (String target : targets) {
            var scan = new Scan(target, scanGroup);
            scanGroup.addScan(scan);
            scanRepository.save(scan);

            try {
                scanProducer.enviarMensagem(new ScanMessage(scan.getId(), target, scanGroup.getId()));
                publishResults.add(new ScanPublishResult(target, scan.getId(), ScanPublishStatus.CONFIRMED, "Publicação confirmada pelo RabbitMQ."));

            } 
            catch (ScanPublishNotConfirmedException e) {
                log.warn("Publicação não confirmada para target {}: {}", target, e.getMessage(), e);
                publishResults.add(new ScanPublishResult(target, scan.getId(), ScanPublishStatus.NOT_CONFIRMED, e.getMessage()));
            } 
            catch (AmqpException e) {
                log.error("Falha na publicação para target {}: {}", target, e.getMessage(), e);
                publishResults.add(new ScanPublishResult(target, scan.getId(), ScanPublishStatus.FAILED, e.getMessage()));
            }
        }

        long confirmedCount = publishResults.stream().filter(r -> r.status() == ScanPublishStatus.CONFIRMED).count();
        ScanStatus status;
        String responseMessage;

        if (confirmedCount == 0) {
            status = ScanStatus.FAILED;
            responseMessage = "Nenhuma solicitação de scan foi confirmada pelo RabbitMQ.";
        } 
        else if (confirmedCount < targets.size()) {
            status = ScanStatus.PARTIAL;
            responseMessage = "Nem todas as solicitações de scan foram confirmadas.";
        } 
        else {
            status = ScanStatus.ACCEPTED;
            responseMessage = "Solicitações de scan aceitas para processamento.";
        }
        
        scanExecutionGroupRepository.save(scanGroup);

        return new ScanResponse(
            responseMessage,
            scanGroup.getId(),
            status,
            targets.size(),
            publishResults
        );
    }

    /**
     * Converte a string recebida em {@code targets} numa lista de hosts,
     * rejeitando qualquer entrada que não seja um IPv4, IPv6 ou range CIDR
     * válido. Isso impede que alguém use esse campo para injetar flags ou
     * argumentos extras no comando do Nmap (ex.: "127.0.0.1 --script=...").
     */
    public List<String> targetsToList(String targets) {
        targets = targets.replaceAll("\\s+", "");
        List<String> list = new ArrayList<>(new LinkedHashSet<>(Arrays.asList(targets.split(","))));

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
