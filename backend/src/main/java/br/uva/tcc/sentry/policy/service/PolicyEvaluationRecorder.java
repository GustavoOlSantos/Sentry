package br.uva.tcc.sentry.policy.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.policy.checker.RuleEvaluationResult;
import br.uva.tcc.sentry.policy.domain.PolicyEvaluation;
import br.uva.tcc.sentry.policy.repository.PolicyEvaluationRepository;
import br.uva.tcc.sentry.scan.domain.Scan;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PolicyEvaluationRecorder {

    private final PolicyEvaluationRepository repository;

    @Transactional
    public int record(Scan scan, List<RuleEvaluationResult> results,
                      Map<String, Finding> findingsByFingerprint, LocalDateTime evaluatedAt) {
        repository.deleteByScanId(scan.getId());

        List<PolicyEvaluation> rows = new ArrayList<>();
        for (RuleEvaluationResult result : results) {
            PolicyEvaluation evaluation = new PolicyEvaluation();
            evaluation.setScan(scan);
            evaluation.setAsset(result.asset());
            evaluation.setPolicyRule(result.rule());
            evaluation.setStatus(result.status());
            evaluation.setEvaluatedAt(evaluatedAt);
            evaluation.setEvidence(result.evidence());
            evaluation.setViolations(result.violations());

            if (result.firstViolation() != null && result.firstViolation().getFingerprint() != null) {
                evaluation.setFinding(findingsByFingerprint.get(result.firstViolation().getFingerprint()));
            }
            rows.add(evaluation);
        }

        repository.saveAll(rows);
        return rows.size();
    }
}
