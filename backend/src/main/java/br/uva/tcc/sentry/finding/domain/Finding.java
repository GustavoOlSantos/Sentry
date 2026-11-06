package br.uva.tcc.sentry.finding.domain;

import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.finding.exception.InvalidFindingTransitionException;
import br.uva.tcc.sentry.vulnerability.domain.Vulnerability;
import br.uva.tcc.sentry.policy.domain.PolicyRule;
import br.uva.tcc.sentry.shared.domain.Severity;

import java.util.UUID;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Um problema encontrado em um asset (vulnerabilidade conhecida ou violação de
 * política). É único por {@link #fingerprint}: scans repetidos do mesmo alvo
 * atualizam o mesmo finding em vez de criar duplicatas — quem decide isso é o
 * {@code FindingReconciler}, usando os métodos de ciclo de vida desta classe.
 *
 * Ciclo de vida (resumo):
 * <pre>
 *   OPEN ⇄ IN_PROGRESS → RESOLVED ─ reteste ok ───────→ VALIDATED
 *                           │                               │
 *                           └─ reteste falhou → FAILED_VALIDATION
 *   (qualquer ativo) ─ sumiu dos scans ───────────────→ VALIDATED
 *   VALIDATED ─ voltou a aparecer ────────────────────→ OPEN (reopenCount++)
 *   JUSTIFIED: risco aceito pelo usuário, fica fora da reconciliação
 * </pre>
 */
@Entity
@Table(name = "finding",
       uniqueConstraints = @UniqueConstraint(name = "uk_finding_fingerprint", columnNames = "fingerprint"),
       indexes = @Index(name = "idx_finding_asset_status", columnList = "asset_id, status"))
@Getter 
@Setter 
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Controle de concorrência: o usuário mexe no Board enquanto um scan reconcilia. */
    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    /** Scan que detectou o finding pela primeira vez. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vulnerability_id")
    private Vulnerability vulnerability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_rule_id")
    private PolicyRule policyRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingStatus status;

    @Column(nullable = true, length = 500)
    private String justification;

    @Column(name = "assignee_id")
    private Long AssigneeId;


    /**
     * Identificador estável e curto da regra/vulnerabilidade que gerou o
     * finding — o CVE ID (ex.: "CVE-2021-41773") para findings de
     * vulnerabilidade, ou um código de regra (ex.: "SSH_EXPOSED") para
     * findings de violação de política. É isso que diferencia ocorrências
     * do mesmo tipo entre si de forma programática (agrupar, deduplicar,
     * filtrar), separado do título, que é só o texto pra exibição.
     */
    @Column(length = 60)
    private String code;

    /** Hash de {@code asset + serviço + tipo + code} (ver {@link FindingFingerprint}). */
    @Column(nullable = false, length = 64, updatable = false)
    private String fingerprint;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Primeira vez que o problema foi visto. */
    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    /** Última vez que um scan (com cobertura) ainda viu o problema. */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    /** Quando o problema deixou de existir: marcado como resolvido pelo usuário ou sumiu dos scans. */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Quando a ferramenta confirmou a correção (base do MTTR: validatedAt - detectedAt). */
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    /** Quantas vezes um finding já validado voltou a aparecer. */
    @Column(name = "reopen_count", nullable = false)
    private int reopenCount;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    /**
     * Mudança de status pedida pelo usuário (Board). Só aceita OPEN,
     * IN_PROGRESS, RESOLVED e JUSTIFIED; VALIDATED e FAILED_VALIDATION são da
     * ferramenta, e findings já validados (fechados) não podem ser mexidos.
     */
    public void changeStatusByUser(FindingStatus newStatus, String justification, LocalDateTime now) {
        if (newStatus == null || !newStatus.isUserSettable()) {
            throw new InvalidFindingTransitionException(
                    "O status '" + newStatus + "' só pode ser definido pela própria ferramenta.");
        }
        if (this.status == FindingStatus.VALIDATED) {
            throw new InvalidFindingTransitionException(
                    "Finding já validado (fechado) não pode ser alterado manualmente. "
                            + "Se o problema voltar, o próximo scan o reabre.");
        }

        boolean hasJustification = justification != null && !justification.isBlank();

        if (newStatus == FindingStatus.JUSTIFIED && !hasJustification) {
            throw new InvalidFindingTransitionException(
                    "É obrigatório informar uma justificativa para marcar o finding como JUSTIFIED.");
        }
        if (hasJustification) {
            this.justification = justification.trim();
        }

        switch (newStatus) {
            case RESOLVED -> {
                if (this.status != FindingStatus.RESOLVED) {
                    this.resolvedAt = now;
                }
                this.validatedAt = null;
            }
            default -> { // OPEN, IN_PROGRESS, JUSTIFIED: o problema ainda "existe" para a ferramenta
                this.resolvedAt = null;
                this.validatedAt = null;
            }
        }

        this.status = newStatus;
    }

    /**
     * O scan reencontrou este finding. Atualiza os dados e aplica a regra:
     * RESOLVED que continua lá → FAILED_VALIDATION (reteste falhou);
     * VALIDATED que voltou → reaberto. Os demais só atualizam {@code lastSeenAt}.
     *
     * @param scanStartedAt quando o scan começou — impede que um scan antigo,
     *                      que terminou depois da ação do usuário, decida por ela.
     */
    public void onSeenAgain(Finding observed, LocalDateTime scanStartedAt, LocalDateTime now) {
        refreshDetailsFrom(observed);
        this.lastSeenAt = now;

        switch (this.status) {
            case RESOLVED -> {
                if (startedAfter(this.resolvedAt, scanStartedAt)) {
                    this.status = FindingStatus.FAILED_VALIDATION;
                }
            }
            case VALIDATED -> {
                if (startedAfter(this.validatedAt, scanStartedAt)) {
                    reopen();
                }
            }
            default -> {
                // OPEN, IN_PROGRESS, FAILED_VALIDATION e JUSTIFIED só renovam o lastSeenAt
            }
        }
    }

    /**
     * O scan NÃO viu este finding e tinha cobertura para vê-lo (quem garante
     * isso é o {@code ScanCoverage}). Para os status ativos, isso significa
     * que o problema foi corrigido: vira VALIDATED.
     */
    public void onNotSeen(LocalDateTime scanStartedAt, LocalDateTime now) {
        if (!absenceIsFresh(scanStartedAt)) {
            return; // outro scan enxergou o problema depois que este começou: ausência velha
        }

        switch (this.status) {
            case OPEN, IN_PROGRESS -> validate(now);
            case RESOLVED, FAILED_VALIDATION -> {
                if (startedAfter(this.resolvedAt, scanStartedAt)) {
                    validate(now);
                }
            }
            default -> {
                // VALIDATED já está fechado; JUSTIFIED é risco aceito
            }
        }
    }

    private void validate(LocalDateTime now) {
        this.status = FindingStatus.VALIDATED;
        this.validatedAt = now;
        if (this.resolvedAt == null) {
            this.resolvedAt = now;
        }
    }

    private void reopen() {
        this.status = FindingStatus.OPEN;
        this.validatedAt = null;
        this.resolvedAt = null;
        this.reopenCount++;
    }

    private void refreshDetailsFrom(Finding observed) {
        this.severity = observed.severity;
        this.title = observed.title;
        this.description = observed.description;
        this.evidence = observed.evidence;
        if (observed.vulnerability != null) {
            this.vulnerability = observed.vulnerability;
        }
        if (observed.policyRule != null) {
            this.policyRule = observed.policyRule;
        }
    }

    /** O scan começou depois do marco? Sem marco (ou sem horário do scan), considera que sim. */
    private static boolean startedAfter(LocalDateTime milestone, LocalDateTime scanStartedAt) {
        return milestone == null || scanStartedAt == null || scanStartedAt.isAfter(milestone);
    }

    private boolean absenceIsFresh(LocalDateTime scanStartedAt) {
        return this.lastSeenAt == null || scanStartedAt == null || !this.lastSeenAt.isAfter(scanStartedAt);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Finding {")
                .append("id=").append(id)
                .append(", code=").append(code)
                .append(", type=").append(type)
                .append(", severity=").append(severity)
                .append(", status=").append(status)
                .append(", title='").append(title).append('\'')
                .append(", description='").append(description).append('\'')
                .append(", detectedAt=").append(detectedAt)
                .append('}')
                .toString();
    }
}
