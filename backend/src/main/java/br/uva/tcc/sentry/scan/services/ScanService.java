package br.uva.tcc.sentry.scan.services;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.scanner.ScanEngine;

@Service
public class ScanService {

    private final ScanEngine scanEngine;

    public ScanService(ScanEngine scanEngine) {
        this.scanEngine = scanEngine;
    }

    public ArrayList<Scan> findAll() {
        return new ArrayList<>();
    }

    public Scan findById(UUID id) {
        return new Scan();
    }

    public ScanResult executeScan(String target) {
        return scanEngine.scan(target);
    }
}
