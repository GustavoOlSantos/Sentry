package br.uva.tcc.sentry.scan.DTO;

import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.scan.domain.ScanStatus;
import br.uva.tcc.sentry.scan.messaging.ScanPublishResult;


public record ScanResponse(String message, UUID groupId, ScanStatus status, int totalTargets, List<ScanPublishResult> publicacoes) {}

