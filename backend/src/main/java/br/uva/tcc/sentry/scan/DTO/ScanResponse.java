package br.uva.tcc.sentry.scan.DTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.uva.tcc.sentry.scan.domain.ScanStatus;

public record ScanResponse(String message, ScanStatus status, Integer hostsCount, Map<String, UUID> scanIds, List<String> failedTargets) {
    
}
