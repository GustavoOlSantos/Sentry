package br.uva.tcc.sentry.policy.checker;

import java.util.ArrayList;
import java.util.List;

import br.uva.tcc.sentry.finding.domain.Finding;

public record CheckOutcome(List<Finding> findings, List<RuleEvaluationResult> evaluations) {

    public static CheckOutcome of(List<Finding> findings) {
        return new CheckOutcome(findings, List.of());
    }

    public static CheckOutcome empty() {
        return new CheckOutcome(List.of(), List.of());
    }

    public CheckOutcome plus(CheckOutcome other) {
        List<Finding> allFindings = new ArrayList<>(findings);
        allFindings.addAll(other.findings);

        List<RuleEvaluationResult> allEvaluations = new ArrayList<>(evaluations);
        allEvaluations.addAll(other.evaluations);

        return new CheckOutcome(allFindings, allEvaluations);
    }
}
