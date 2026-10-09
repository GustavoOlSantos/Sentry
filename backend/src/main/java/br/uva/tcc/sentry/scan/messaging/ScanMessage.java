package br.uva.tcc.sentry.scan.messaging;

import  java.util.UUID;

public record ScanMessage(UUID scanId, String target) {
    
}
