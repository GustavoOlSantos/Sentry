package br.uva.tcc.sentry.finding.checker;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.finding.domain.ConfigFinding;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;

/**
 * Sinaliza exposição da porta SSH (22). Confirma apenas que a porta está
 * acessível pela rede escaneada — checar se {@code PermitRootLogin} está
 * habilitado exigiria uma tentativa de autenticação, fora do escopo de um
 * scanner não intrusivo.
 */
@Component
public class ExposedSshCheck implements ConfigCheck {

    private static final int SSH_PORT = 22;

    @Override
    public List<ConfigFinding> check(DiscoveredHost host) {
        List<ConfigFinding> findings = new ArrayList<>();
        for (Service port : host.services()) {
            if (port.getPort() == SSH_PORT && "open".equals(port.getState())) {
                findings.add(new ConfigFinding(
                        "SSH_EXPOSED",
                        host.asset().getIpAddress(),
                        SSH_PORT,
                        "MEDIUM",
                        "Porta SSH (22) acessível pela rede escaneada. Não confirma se root login está habilitado "
                                + "(isso exigiria tentativa de autenticação, fora do escopo deste scanner), mas é um "
                                + "ponto de atenção: recomendado restringir por IP de origem, usar autenticação por "
                                + "chave e desabilitar PermitRootLogin manualmente."
                ));
            }
        }
        return findings;
    }
}