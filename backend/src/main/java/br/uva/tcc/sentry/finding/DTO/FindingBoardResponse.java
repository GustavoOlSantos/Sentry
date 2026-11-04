package br.uva.tcc.sentry.finding.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import br.uva.tcc.sentry.Asset.DTO.AssetReferenceResponse;
import br.uva.tcc.sentry.Asset.DTO.ServiceResponse;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.shared.domain.Severity;

public record FindingBoardResponse(
    UUID id,
    FindingType type,
    Severity severity,
    FindingStatus status,
    String title,
    String description,
    LocalDateTime detectedAt,
    LocalDateTime resolvedAt,
    String evidence,
    String justification,
    Long assigneeId,
    AssetReferenceResponse asset,
    ServiceResponse service
) {
    public static FindingBoardResponse from(Finding finding) {
        return new FindingBoardResponse(
            finding.getId(),
            finding.getType(),
            finding.getSeverity(),
            finding.getStatus(),
            finding.getTitle(),
            finding.getDescription(),
            finding.getDetectedAt(),
            finding.getResolvedAt(),
            finding.getEvidence(),
            finding.getJustification(),
            finding.getAssigneeId(),
            AssetReferenceResponse.from(finding.getAsset()),
            ServiceResponse.from(finding.getService())
        );
    }
}
