package br.uva.tcc.sentry.policy.domain;

import br.uva.tcc.sentry.shared.domain.Severity;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * Representa uma regra de política de segurança
 */

@Getter 
@Setter 
public class PolicyRule {
    
    private Long id;
    private Policy policy;
    private String name;
    private String description;
    private PolicyRuleType type;
    private String condiditon;
    private String expectedValue;
    private Severity severity;
    private Boolean enabled;
}
