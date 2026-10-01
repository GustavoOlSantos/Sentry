package br.uva.tcc.sentry.finding.domain;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability;
import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.shared.domain.Severity;

import java.util.UUID;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;


/**
 * Representa uma ocorrência efetivamente encontrada no sistema, seja ela uma vulnerabilidade ou uma anomalia
 * É o resultado concreto da análise de um {@link br.uva.tcc.sentry.Asset.domain.Asset Asset} e {@link br.uva.tcc.sentry.Asset.domain.Service Service} específico
 */
@Getter 
@Setter 
public class Finding {
    
    private UUID id;
    private Asset asset;
    private Service service;
    private Vulnerability vulnerability;
    private PolicyRule policyRule;
    private FindingType type;
    private Severity severity;
    private FindingStatus  status;
    private String title;
    private String description;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String evidence;

}
