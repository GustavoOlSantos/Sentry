package br.uva.tcc.sentry.finding.domain;

import java.time.LocalDateTime;

import br.uva.tcc.sentry.scan.domain.Scan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Uma transição de status de um {@link Finding}. É o que permite reconstruir o estado
 * de cada finding em qualquer momento do passado (relatório de evolução do risco), em
 * vez de inferi-lo a partir do status atual.
 */
@Entity
@Table(name = "finding_status_history", indexes = {
        @Index(name = "idx_fsh_finding_changed", columnList = "finding_id, changed_at"),
        @Index(name = "idx_fsh_to_status_changed", columnList = "to_status, changed_at"),
        @Index(name = "idx_fsh_scan", columnList = "scan_id")
})
@Getter
@Setter
@NoArgsConstructor
public class FindingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private FindingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private FindingStatus toStatus;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HistoryOrigin origin;

    public static FindingStatusHistory of(Finding finding, FindingStatus from, FindingStatus to,
                                          LocalDateTime changedAt, Long userId, Scan scan, HistoryOrigin origin) {
        FindingStatusHistory entry = new FindingStatusHistory();
        entry.finding = finding;
        entry.fromStatus = from;
        entry.toStatus = to;
        entry.changedAt = changedAt;
        entry.changedByUserId = userId;
        entry.scan = scan;
        entry.origin = origin;
        return entry;
    }
}
