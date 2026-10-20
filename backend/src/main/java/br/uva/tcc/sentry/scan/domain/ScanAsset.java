package br.uva.tcc.sentry.scan.domain;

import br.uva.tcc.sentry.Asset.domain.Asset;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

/**
 * Associação entre um {@link Scan} e os {@link Asset}s que ele encontrou
 * (N:N com atributos próprios: quando foi descoberto e em qual estado).
 */
@Entity
@Table(name = "scan_asset",
       uniqueConstraints = @UniqueConstraint(name = "uk_scan_asset", columnNames = {"scan_id", "asset_id"}))
@Getter
@Setter
public class ScanAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "discovered_at")
    private LocalDateTime discoveredAt;

    @Column(length = 30)
    private String status;
}
