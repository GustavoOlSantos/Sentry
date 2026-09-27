package br.uva.tcc.sentry.finding.domain;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class PortInfo {

    private int port;
    private String protocol;   // tcp / udp
    private String state;      // open / closed / filtered
    private String service;    // ex: http, ssh, mysql
    private String product;    // ex: Apache httpd
    private String version;    // ex: 2.4.29
    private String cpe;        // ex: cpe:2.3:a:apache:http_server:2.4.29:*:*:*:*:*:*:*

    public PortInfo() {
    }

    public PortInfo(int port, String protocol, String state, String service,
                     String product, String version, String cpe) {
        this.port = port;
        this.protocol = protocol;
        this.state = state;
        this.service = service;
        this.product = product;
        this.version = version;
        this.cpe = cpe;
    }

    /** Considerado "identificável" para efeito de consulta ao NVD. */
    public boolean hasCpe() {
        return cpe != null && !cpe.isBlank();
    }

    @Override
    public String toString() {
        return port + "/" + protocol + " " + state + " " + service
                + (product != null ? " (" + product + " " + version + ")" : "");
    }
}

