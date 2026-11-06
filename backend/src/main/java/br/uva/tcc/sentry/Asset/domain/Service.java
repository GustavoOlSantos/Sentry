package br.uva.tcc.sentry.Asset.domain;

import java.util.UUID;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

/** 
 * Representa  um serviço encontrado em uma porta.
 */
@Entity
@Table(name = "service",
       uniqueConstraints = @UniqueConstraint(name = "uk_service_asset_port_protocol", columnNames = {"asset_id", "port", "protocol"}))
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

    public Service(UUID id, int port, String protocol, String state, String service,
                     String product, String version, String cpe) {
        this.id = id;
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

    public static final String STATE_OPEN = "open";
    public static final String STATE_CLOSED = "closed";

    public boolean isOpen() {
        return STATE_OPEN.equals(state);
    }

    /** A porta deixou de responder. A linha é mantida porque findings antigos apontam para ela. */
    public void markClosed() {
        this.state = STATE_CLOSED;
    }

    /** Identidade do serviço dentro de um asset: porta + protocolo (ex.: "443/tcp"). */
    public String key() {
        return port + "/" + protocol;
    }

    /** Atualiza com o que o scan mais recente observou nesta mesma porta. */
    public void updateFrom(Service observed) {
        this.state = observed.state;
        this.serviceName = observed.serviceName;
        this.product = observed.product;
        this.version = observed.version;
        this.extraInfo = observed.extraInfo;
        this.cpe = observed.cpe;
    }

    @Override
    public String toString() {
        return port + "/" + protocol + " " + state + " " + serviceName
                + (product != null ? " (" + product + " " + version + ")" : "");
    }
}
