package br.uva.tcc.sentry.scan.domain;

import java.util.ArrayList;
import java.util.List;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ScanResult {

    private String scanId;
    private final String command;
    private List<DiscoveredHost> hosts = new ArrayList<>();
    private List<Finding> findings = new ArrayList<>();
    private List<Vulnerability> vulnerabilities = new ArrayList<>();

    public ScanResult(String command) {
        this.command = command;
    }

    public void addHost(DiscoveredHost host) {
        this.hosts.add(host);
    }

    public void addVulnerabilities(List<Vulnerability> vs) {
        this.vulnerabilities.addAll(vs);
    }

    public void addFinding(Finding f) {
        this.findings.add(f);
    }

    public void addFindings(List<Finding> findings) {
        this.findings.addAll(findings);
    }

}