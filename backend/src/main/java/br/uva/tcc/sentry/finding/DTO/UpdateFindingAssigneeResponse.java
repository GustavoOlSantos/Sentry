package br.uva.tcc.sentry.finding.DTO;

import java.util.UUID;

public record UpdateFindingAssigneeResponse(Long assigneeId, UUID findingId, String message) {}