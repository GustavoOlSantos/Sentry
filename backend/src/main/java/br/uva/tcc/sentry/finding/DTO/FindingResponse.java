package br.uva.tcc.sentry.finding.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.shared.domain.Severity;

public record FindingResponse(
    UUID id,
    UUID scanId,
    UUID serviceId,
    Long vulnerabilityId,
    Long policyRuleId,
    FindingType type,
    Severity severity,
    FindingStatus status,
    String title,
    String description,
    LocalDateTime detectedAt,
    LocalDateTime resolvedAt,
    String evidence
) {
    public static FindingResponse from(Finding finding) {
        return new FindingResponse(
            finding.getId(),
            finding.getScan() != null ? finding.getScan().getId() : null,
            finding.getService() != null ? finding.getService().getId() : null,
            finding.getVulnerability() != null ? finding.getVulnerability().getId() : null,
            finding.getPolicyRule() != null ? finding.getPolicyRule().getId() : null,
            finding.getType(),
            finding.getSeverity(),
            finding.getStatus(),
            finding.getTitle(),
            finding.getDescription(),
            finding.getDetectedAt(),
            finding.getResolvedAt(),
            finding.getEvidence()
        );
    }
}