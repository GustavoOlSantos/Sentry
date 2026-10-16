package br.uva.tcc.sentry.scan.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.finding.domain.Finding;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ScanResult {

    private UUID scanId;
    private final String command;
    private List<DiscoveredHost> hosts = new ArrayList<>();
    private List<Finding> findings = new ArrayList<>();

    public ScanResult(String command) {
        this.command = command;
    }

    public void addHost(DiscoveredHost host) {
        this.hosts.add(host);
    }

    public void addFinding(Finding f) {
        this.findings.add(f);
    }

    public void addFindings(List<Finding> findings) {
        this.findings.addAll(findings);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n========== SCAN RESULT ==========\n")
        .append("Scan ID: ").append(scanId).append('\n')
        .append("Comando: ").append(command).append('\n');

        sb.append("\n--- Hosts encontrados (")
        .append(hosts.size()).append(") ---\n");

        if (hosts.isEmpty()) {
            sb.append("Nenhum host encontrado.\n");
        } else {
            for (int i = 0; i < hosts.size(); i++) {
                sb.append(i + 1).append(". ")
                .append(hosts.get(i)).append('\n');
            }
        }

        sb.append("\n--- Findings (")
        .append(findings.size()).append(") ---\n");

        if (findings.isEmpty()) {
            sb.append("Nenhum finding encontrado.\n");
        } else {
            for (int i = 0; i < findings.size(); i++) {
                sb.append(i + 1).append(". ")
                .append(findings.get(i)).append('\n');
            }
        }
        return sb.append("=================================\n").toString();
    }

}