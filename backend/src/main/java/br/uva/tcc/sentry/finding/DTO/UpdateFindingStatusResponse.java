package br.uva.tcc.sentry.finding.DTO;

import java.util.UUID;

import br.uva.tcc.sentry.finding.domain.FindingStatus;


public record UpdateFindingStatusResponse(UUID findingId, FindingStatus previousStatus, FindingStatus newStatus, String message) {

}
