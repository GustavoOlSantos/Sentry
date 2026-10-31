package br.uva.tcc.sentry.finding.factory;

import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.shared.domain.Severity;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class FindingFactory {

    public Finding fromVulnerability(DiscoveredHost host, Service service, Vulnerability vulnerability, Scan scan) {
        var finding = createBaseFinding(host, service, FindingType.VULNERABILITY, vulnerability.getSeverity(), scan);

        finding.setVulnerability(vulnerability);
        finding.setCode(vulnerability.getCveId());
        finding.setTitle(vulnerability.getTitle());
        finding.setDescription(vulnerability.getDescription());
        finding.setEvidence(buildVulnerabilityEvidence(service, vulnerability));

        return finding;
    }

    /**
     * Caminho pra quando já existe uma {@link PolicyRule} persistida (regra
     * dinâmica, configurada pelo usuário). Hoje nenhum checker usa isso
     * ainda — é o caminho que o futuro avaliador dinâmico de políticas vai
     * usar, sem precisar de nenhuma mudança aqui na fábrica.
     */
    public Finding fromPolicyViolation(DiscoveredHost host, Service service, PolicyRule rule, String evidence, Scan scan) {
        var finding = createBaseFinding(host, service, FindingType.POLICY_VIOLATION, rule.getSeverity(), scan);

        finding.setPolicyRule(rule);
        finding.setCode(rule.getName());
        finding.setTitle(rule.getName());
        finding.setDescription(rule.getDescription());
        finding.setEvidence(evidence);

        return finding;
    }

    /**
     * Caminho usado pelas checagens de configuração fixas (SSH exposto,
     * porta de banco exposta, certificado expirado...), que ainda não são
     * regras dinâmicas vindas do banco.
     *
     * Substitui o antigo overload "TEMPORÁRIO" (que só recebia
     * severidade/título/descrição soltos, sem código e sem evidência, e
     * descartava o scan). Agora exige os mesmos campos que o caminho
     * baseado em {@link PolicyRule} acima — código estável e evidência —
     * então quando as regras fixas de cada checker forem migradas pra
     * {@code PolicyRule} reais no banco, só é preciso trocar qual overload
     * é chamado; a fábrica e o formato do Finding não mudam.
     */
    public Finding fromPolicyViolation(DiscoveredHost host, Service service, String code, String title,
            String description, Severity severity, String evidence, Scan scan) {
        var finding = createBaseFinding(host, service, FindingType.POLICY_VIOLATION, severity, scan);

        finding.setCode(code);
        finding.setTitle(title);
        finding.setDescription(description);
        finding.setEvidence(evidence);

        return finding;
    }

    private String buildVulnerabilityEvidence(Service service, Vulnerability vulnerability) {
        StringBuilder evidence = new StringBuilder("Serviço identificado: ");

        if (service.getProduct() != null && !service.getProduct().isBlank()) {
            evidence.append(service.getProduct());
            if (service.getVersion() != null && !service.getVersion().isBlank()) {
                evidence.append(' ').append(service.getVersion());
            }
        } else if (service.getServiceName() != null) {
            evidence.append(service.getServiceName());
        } else {
            evidence.append("desconhecido");
        }

        evidence.append(" na porta ").append(service.getPort()).append('/').append(service.getProtocol());

        if (service.hasCpe()) {
            evidence.append(" (CPE: ").append(service.getCpe()).append(')');
        }

        evidence.append(". Referência: https://nvd.nist.gov/vuln/detail/").append(vulnerability.getCveId());

        return evidence.toString();
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
