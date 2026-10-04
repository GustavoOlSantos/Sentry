package br.uva.tcc.sentry.finding.factory;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability; 
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.shared.domain.Severity;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;


@Component
public class FindingFactory {

    public Finding fromVulnerability(DiscoveredHost host, Service service, Vulnerability vulnerability, Scan scan) {
        var finding = createBaseFinding(host, service, FindingType.VULNERABILITY, vulnerability.getSeverity(), scan);

        finding.setVulnerability(vulnerability);
        finding.setTitle(vulnerability.getTitle());
        finding.setDescription(vulnerability.getDescription());

        return finding;
    }

     public Finding fromPolicyViolation(DiscoveredHost host, Service service, PolicyRule rule, String evidence, Scan scan) {
        var finding = createBaseFinding(host, service, FindingType.POLICY_VIOLATION, rule.getSeverity(), scan);

        finding.setPolicyRule(rule);
        finding.setTitle(rule.getName());
        finding.setDescription(rule.getDescription());
        finding.setEvidence(evidence);

        return finding;
    }

    /* TEMPORÁRIO */
    public Finding fromPolicyViolation(DiscoveredHost host, Service service, Severity severity, String title, String description) {
        var finding = createBaseFinding(host, service, FindingType.POLICY_VIOLATION, severity, null);

        finding.setTitle(title);
        finding.setDescription(description);

        return finding;
    }

     private Finding createBaseFinding(DiscoveredHost host, Service service, FindingType type, Severity severity, Scan scan) {
        var finding = new Finding();

        finding.setAsset(host.asset());
        finding.setScan(scan);
        finding.setService(service);
        finding.setType(type);
        finding.setSeverity(severity);
        finding.setStatus(FindingStatus.OPEN);
        finding.setDetectedAt(LocalDateTime.now());

        return finding;
    }
}
