package br.uva.tcc.sentry.finding.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class Host {

    private String ip;
    private String hostname;
    private String os;
    private List<PortInfo> ports = new ArrayList<>();

    public Host() {
    }

    public Host(String ip) {
        this.ip = ip;
    }

    public void addPort(PortInfo port) {
        this.ports.add(port);
    }
}
