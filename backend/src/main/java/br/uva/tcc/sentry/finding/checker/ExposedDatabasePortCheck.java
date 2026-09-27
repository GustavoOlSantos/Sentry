package br.uva.tcc.sentry.finding.checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.finding.domain.ConfigFinding;
import br.uva.tcc.sentry.finding.domain.Host;
import br.uva.tcc.sentry.finding.domain.PortInfo;

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

    @Override
    public List<ConfigFinding> check(Host host) {
        List<ConfigFinding> findings = new ArrayList<>();
        for (PortInfo port : host.getPorts()) {
            String dbName = SENSITIVE_DB_PORTS.get(port.getPort());
            if (dbName != null) {
                findings.add(new ConfigFinding(
                        "EXPOSED_DATABASE_PORT",
                        host.getIp(),
                        port.getPort(),
                        "HIGH",
                        "Porta de " + dbName + " (" + port.getPort() + ") acessível pela rede escaneada. "
                                + "Bancos de dados normalmente não deveriam ser expostos fora da rede interna/VPC. "
                                + "Recomendação: restringir via firewall/security group ao IP dos servidores de aplicação."
                ));
            }
        }
        return findings;
    }
}
