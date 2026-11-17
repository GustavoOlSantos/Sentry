package br.uva.tcc.sentry.policy.checker;

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.policy.domain.PolicyEvaluationStatus;
import br.uva.tcc.sentry.policy.domain.PolicyRule;

public record RuleEvaluationResult(
        PolicyRule rule,
        Asset asset,
        PolicyEvaluationStatus status,
        String evidence,
        int violations,
        Finding firstViolation) {
}
