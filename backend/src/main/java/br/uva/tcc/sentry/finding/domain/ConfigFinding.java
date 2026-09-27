package br.uva.tcc.sentry.finding.domain;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class ConfigFinding {

    private String type;
    private String host;
    private int port;
    private String severity;    // HIGH / MEDIUM / LOW
    private String description;

    public ConfigFinding() {
    }

    public ConfigFinding(String type, String host, int port, String severity, String description) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.severity = severity;
        this.description = description;
    }
}

