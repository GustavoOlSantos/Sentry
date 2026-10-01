package br.uva.tcc.sentry.finding.checker;

import java.util.List;

import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.finding.domain.ConfigFinding;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;

/**
 * Ponto único de entrada para rodar todas as checagens de configuração
 * insegura disponíveis contra um host. O Spring injeta automaticamente
 * todos os beans que implementam {@link ConfigCheck} (Strategy pattern);
 * adicionar uma nova checagem não exige alterar esta classe.
 */
@Service
public class ConfigFindingService {

    private final List<ConfigCheck> checks;

    public ConfigFindingService(List<ConfigCheck> checks) {
        this.checks = checks;
    }

    public List<ConfigFinding> check(DiscoveredHost host) {
        return checks.stream()
                .flatMap(check -> check.check(host).stream())
                .toList();
    }
}