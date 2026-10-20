package br.uva.tcc.sentry.scan.messaging;

import java.util.UUID;

public record ScanPublishResult(String target, UUID scanId, ScanPublishStatus status, String message) {}
