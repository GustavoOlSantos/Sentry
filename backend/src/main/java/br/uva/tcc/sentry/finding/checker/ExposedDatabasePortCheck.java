package br.uva.tcc.sentry.finding.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.factory.FindingFactory;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;
import br.uva.tcc.sentry.shared.domain.Severity;

/**
 * Sinaliza portas de banco de dados que normalmente não deveriam estar
 * acessíveis publicamente, apenas internamente na rede/VPC.
 */
@Component
public class ExposedDatabasePortCheck implements ConfigCheck {

    private static final Map<Integer, String> SENSITIVE_DB_PORTS = Map.of(
            3306, "MySQL/MariaDB",
            5432, "PostgreSQL",
            1433, "Microsoft SQL Server",
            27017, "MongoDB",
            6379, "Redis",
            9200, "Elasticsearch"
    );

    private final FindingFactory findingFactory;

    public ExposedDatabasePortCheck(FindingFactory findingFactory) {
        this.findingFactory = findingFactory;
    }

    @Override
    public List<Finding> check(DiscoveredHost host) {
        List<Finding> findings = new ArrayList<>();

        for (Service service : host.services()) {
            String dbName = SENSITIVE_DB_PORTS.get(service.getPort());

            if (dbName != null) {
                findings.add(
                    findingFactory.fromPolicyViolation(
                        host,
                        service,
                        Severity.HIGH,
                        "EXPOSED_DATABASE_PORT",
                        "Porta de " + dbName + " (" + service.getPort() + ") acessível pela rede escaneada. "
                            + "Bancos de dados normalmente não deveriam ser expostos fora da rede interna/VPC. "
                            + "Recomendação: restringir via firewall/security group ao IP dos servidores de aplicação."
                    )
                );
            }
        }

        return findings;
    }
}