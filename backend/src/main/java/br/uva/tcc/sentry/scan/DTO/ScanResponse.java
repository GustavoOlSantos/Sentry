package br.uva.tcc.sentry.scan.DTO;

import br.uva.tcc.sentry.scan.domain.ScanResult;
import br.uva.tcc.sentry.scan.domain.Scan;

public record ScanResponse(Scan scan, ScanResult scanResult) {
    
}
