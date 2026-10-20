package br.uva.tcc.sentry.Asset.domain;

import java.util.UUID;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "asset", indexes = @Index(name = "idx_asset_ip_address", columnList = "ip_address"))
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
}
