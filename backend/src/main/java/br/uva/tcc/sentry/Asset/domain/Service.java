package br.uva.tcc.sentry.Asset.domain;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** 
 * Representa  um serviço encontrado em uma porta.
 */
@Getter
@Setter
public class Service {
    
    UUID id;
    Asset asset;
    Integer port;
    String protocol;
    String state;
    String serviceName;
    String product;
    String version;
    String extraInfo;
    String cpe;

    public Service(int port, String protocol, String state, String service,
                     String product, String version, String cpe) {
        this.port = port;
        this.protocol = protocol;
        this.state = state;
        this.serviceName = service;
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
        return port + "/" + protocol + " " + state + " " + serviceName
                + (product != null ? " (" + product + " " + version + ")" : "");
    }
}
