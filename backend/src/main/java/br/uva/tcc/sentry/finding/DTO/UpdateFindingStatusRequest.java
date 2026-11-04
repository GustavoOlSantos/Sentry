package br.uva.tcc.sentry.finding.DTO;

import br.uva.tcc.sentry.finding.domain.FindingStatus;

public record UpdateFindingStatusRequest(
    FindingStatus status,
    String justification
) {}