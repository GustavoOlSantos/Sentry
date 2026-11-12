package br.uva.tcc.sentry.scan.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.scan.domain.ScanExecutionGroup;
import br.uva.tcc.sentry.scan.domain.ScanResult;

import br.uva.tcc.sentry.scan.repository.ScanExecutionGroupRepository;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;
import br.uva.tcc.sentry.scan.validation.TargetValidator;

/**
 * Orquestra o ciclo completo de um scan: dispara o {@link ScanEngine}
 * (hoje, Nmap) para descobrir hosts/portas, consulta o NVD por CVEs
 * conhecidas em cada porta identificada por CPE e roda as checagens de
 * configuração insegura em cada host — juntando tudo no {@link ScanResult}.
 */
@Service
public class ScanExecGroupService {

    private final ScanExecutionGroupRepository scanExecutionGroupRepository;

    public ScanExecGroupService(TargetValidator targetValidator, ScanExecutionGroupRepository scanExecutionGroupRepository) {
        this.scanExecutionGroupRepository = scanExecutionGroupRepository;
    }

    public List<ScanExecutionGroup> findAll() {
        return scanExecutionGroupRepository.findAll();
    }

    public Optional<ScanExecutionGroup> findById(UUID id) {
        return scanExecutionGroupRepository.findById(id);
    }

    public void deleteById(UUID id){
        scanExecutionGroupRepository.deleteById(id);
    }

    public ScanExecutionGroup editScan(ScanExecutionGroup scanGroup){
        return scanExecutionGroupRepository.save(scanGroup);
    }
}
