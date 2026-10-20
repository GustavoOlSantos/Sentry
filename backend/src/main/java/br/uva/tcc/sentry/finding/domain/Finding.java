package br.uva.tcc.sentry.finding.domain;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability;
import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.shared.domain.Severity;

import java.util.UUID;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "finding")
@Getter 
@Setter 
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id")
    private Vulnerability vulnerability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_rule_id")
    private PolicyRule policyRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingStatus status;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Finding {")
                .append("id=").append(id)
                .append(", type=").append(type)
                .append(", severity=").append(severity)
                .append(", status=").append(status)
                .append(", title='").append(title).append('\'')
                .append(", description='").append(description).append('\'')
                .append(", detectedAt=").append(detectedAt)
                .append('}')
                .toString();
    }
}
