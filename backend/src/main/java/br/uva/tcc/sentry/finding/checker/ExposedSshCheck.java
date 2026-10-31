package br.uva.tcc.sentry.finding.checker;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.shared.domain.Severity;

/**
 * Sinaliza exposição da porta SSH (22). Confirma apenas que a porta está
 * acessível pela rede escaneada — checar se {@code PermitRootLogin} está
 * habilitado exigiria uma tentativa de autenticação, fora do escopo de um
 * scanner não intrusivo.
 */
@Component
public class ExposedSshCheck implements ConfigCheck {

    private static final int SSH_PORT = 22;

    private static final String CODE = "SSH_EXPOSED";
    private static final String TITLE = "Porta SSH exposta";
    private static final String DESCRIPTION =
            "Porta SSH acessível pela rede escaneada. Não confirma se root login está habilitado "
                    + "(isso exigiria tentativa de autenticação, fora do escopo deste scanner), mas é um "
                    + "ponto de atenção: recomendado restringir por IP de origem, usar autenticação por "
                    + "chave e desabilitar PermitRootLogin manualmente.";

    private final FindingFactory findingFactory;

    public ExposedSshCheck(FindingFactory findingFactory) {
        this.findingFactory = findingFactory;
    }

    @Override
    public List<Finding> check(DiscoveredHost host) {
        List<Finding> findings = new ArrayList<>();

        for (Service service : host.services()) {
            if (service.getPort() == SSH_PORT && "open".equals(service.getState())) {
                String evidence = "Porta " + service.getPort() + "/" + service.getProtocol()
                        + " aberta em " + host.asset().getIpAddress();

                findings.add(
                    findingFactory.fromPolicyViolation(
                        host,
                        service,
                        CODE,
                        TITLE,
                        DESCRIPTION,
                        Severity.MEDIUM,
                        evidence,
                        null
                    )
                );
            }
        }

        return findings;
    }
}
