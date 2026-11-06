package br.uva.tcc.sentry.Asset.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.uva.tcc.sentry.finding.domain.Finding;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "asset",
       uniqueConstraints = @UniqueConstraint(name = "uk_asset_ip_address", columnNames = "ip_address"))
@Getter
@Setter
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ip_address", nullable = false, length = 45) // 45 = tamanho máximo de um IPv6 textual
    private String ipAddress;

    private String hostname;

    @Column(name = "mac_address", length = 17)
    private String macAddress;

    @Column(name = "operating_system")
    private String operatingSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @OneToMany(mappedBy = "asset", fetch = FetchType.LAZY)
    private List<Service> services = new ArrayList<>();

    @OneToMany(mappedBy = "asset", fetch = FetchType.LAZY)
    private List<Finding> findings = new ArrayList<>();

    /**
     * Atualiza o asset com o que o scan mais recente observou. Não apaga com
     * null o que já se sabia (hostname/MAC/SO nem sempre são detectados).
     */
    public void refreshFrom(Asset observed, LocalDateTime seenAt) {
        if (observed.hostname != null) {
            this.hostname = observed.hostname;
        }
        if (observed.macAddress != null) {
            this.macAddress = observed.macAddress;
        }
        if (observed.operatingSystem != null) {
            this.operatingSystem = observed.operatingSystem;
        }
        if (observed.status != null) {
            this.status = observed.status;
        }
        if (this.firstSeenAt == null) {
            this.firstSeenAt = seenAt;
        }
        this.lastSeenAt = seenAt;
    }
}
