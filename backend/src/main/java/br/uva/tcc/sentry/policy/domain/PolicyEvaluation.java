package br.uva.tcc.sentry.policy.domain;

import java.time.LocalDateTime;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.scan.domain.Scan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "policy_evaluation",
        uniqueConstraints = @UniqueConstraint(name = "uk_policy_eval_scan_asset_rule",
                columnNames = { "scan_id", "asset_id", "policy_rule_id" }),
        indexes = {
                @Index(name = "idx_policy_eval_asset_rule_at", columnList = "asset_id, policy_rule_id, evaluated_at"),
                @Index(name = "idx_policy_eval_evaluated_at", columnList = "evaluated_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class PolicyEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_rule_id", nullable = false)
    private PolicyRule policyRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyEvaluationStatus status;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(nullable = false)
    private int violations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id")
    private Finding finding;
}
