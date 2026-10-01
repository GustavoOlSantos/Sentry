package br.uva.tcc.sentry.scan.domain;

import java.util.ArrayList;
import java.util.List;

import br.uva.tcc.sentry.finding.domain.ConfigFinding;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ScanResult {

    private String scanId;
    private String targets;
    private final String command;
    private List<DiscoveredHost> hosts = new ArrayList<>();
    private List<Vulnerability> vulnerabilities = new ArrayList<>();
    private List<ConfigFinding> configFindings = new ArrayList<>();

    public ScanResult(String targets, String command) {
        this.targets = targets;
        this.command = command;
    }

    public void addHost(DiscoveredHost host) {
        this.hosts.add(host);
    }

    public void addVulnerabilities(List<Vulnerability> vs) {
        this.vulnerabilities.addAll(vs);
    }

    public void addConfigFinding(ConfigFinding f) {
        this.configFindings.add(f);
    }

    public void addConfigFindings(List<ConfigFinding> findings) {
        this.configFindings.addAll(findings);
    }

}