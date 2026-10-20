package br.uva.tcc.sentry.policy.domain;

import br.uva.tcc.sentry.shared.domain.Severity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "policy_rule")
@Getter 
@Setter 
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyRuleType type;

    @Column(name = "rule_condition")
    private String condiditon;

    @Column(name = "expected_value")
    private String expectedValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    private Boolean enabled;
}
