package br.uva.tcc.sentry.Asset.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/** 
 * Representa  um serviço encontrado em uma porta.
 */
@Entity
@Table(name = "service")
@Getter
@Setter
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(nullable = false)
    private Integer port;

    @Column(length = 10)
    private String protocol;

    @Column(length = 20)
    private String state;

    @Column(name = "service_name")
    private String serviceName;

    private String product;

    private String version;

    @Column(name = "extra_info")
    private String extraInfo;

    @Column(length = 512)
    private String cpe;

    protected Service() {
    }

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

    public boolean hasCpe() {
        return cpe != null && !cpe.isBlank();
    }

    @Override
    public String toString() {
        return port + "/" + protocol + " " + state + " " + serviceName
                + (product != null ? " (" + product + " " + version + ")" : "");
    }
}
