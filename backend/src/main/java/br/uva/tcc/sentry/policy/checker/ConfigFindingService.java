package br.uva.tcc.sentry.policy.checker;

import java.util.List;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.scan.domain.Scan;

/**
 * Ponto único de entrada para executar todas as checagens de configuração
 * disponíveis contra um host.
 *
 * O Spring injeta automaticamente todos os beans que implementam
 * {@link ConfigCheck} (Strategy pattern). Adicionar uma nova checagem
 * não exige alterar esta classe.
 */
@Service
public class ConfigFindingService {

    private final List<ConfigCheck> checks;

    public ConfigFindingService(List<ConfigCheck> checks) {
        this.checks = checks;
    }

    public List<Finding> check(DiscoveredHost host, Scan scan) {
        return evaluate(host, scan).findings();
    }

    public CheckOutcome evaluate(DiscoveredHost host, Scan scan) {
        CheckOutcome outcome = CheckOutcome.empty();

        for (ConfigCheck check : checks) {
            outcome = outcome.plus(check.evaluate(host));
        }

        outcome.findings().forEach(finding -> finding.setScan(scan));

        return outcome;
    }
}