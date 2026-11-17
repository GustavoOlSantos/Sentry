package br.uva.tcc.sentry.finding.domain;

import java.time.LocalDateTime;

import br.uva.tcc.sentry.scan.domain.Scan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registra cada finding observado por um scan (novos + reencontrados).
 */
@Entity
@Table(name = "scan_finding",
        uniqueConstraints = @UniqueConstraint(name = "uk_scan_finding", columnNames = { "scan_id", "finding_id" }),
        indexes = @Index(name = "idx_scan_finding_finding", columnList = "finding_id"))
@Getter
@Setter
@NoArgsConstructor
public class ScanFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false)
    private Scan scan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Column(name = "observed_at")
    private LocalDateTime observedAt;

    public ScanFinding(Scan scan, Finding finding, LocalDateTime observedAt) {
        this.scan = scan;
        this.finding = finding;
        this.observedAt = observedAt;
    }
}
