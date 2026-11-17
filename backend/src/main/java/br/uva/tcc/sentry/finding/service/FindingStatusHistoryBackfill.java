package br.uva.tcc.sentry.finding.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingStatusHistory;
import br.uva.tcc.sentry.finding.domain.HistoryOrigin;
import br.uva.tcc.sentry.finding.domain.ScanFinding;
import br.uva.tcc.sentry.finding.repository.FindingStatusHistoryRepository;
import br.uva.tcc.sentry.finding.repository.ScanFindingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/**
 * Garante que findings pré-existentes no banco possuam ao menos uma entrada inicial
 * em {@code finding_status_history} e {@code scan_finding} quando a base já tinha dados
 * antes da implantação do módulo de relatórios.
 */
@Component
@RequiredArgsConstructor
public class FindingStatusHistoryBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FindingStatusHistoryBackfill.class);

    private final FindingStatusHistoryRepository historyRepository;
    private final ScanFindingRepository scanFindingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Finding> missingHistory = entityManager.createQuery(
                "SELECT f FROM Finding f WHERE NOT EXISTS (SELECT 1 FROM FindingStatusHistory h WHERE h.finding = f)",
                Finding.class).getResultList();

        if (!missingHistory.isEmpty()) {
            List<FindingStatusHistory> entries = new ArrayList<>();
            for (Finding f : missingHistory) {
                LocalDateTime detectedAt = f.getDetectedAt() != null ? f.getDetectedAt() : LocalDateTime.now();
                entries.add(FindingStatusHistory.of(f, null, FindingStatus.OPEN, detectedAt, null, f.getScan(), HistoryOrigin.BACKFILL));

                if (f.getStatus() == FindingStatus.VALIDATED && f.getValidatedAt() != null) {
                    entries.add(FindingStatusHistory.of(f, FindingStatus.OPEN, FindingStatus.VALIDATED,
                            f.getValidatedAt(), null, null, HistoryOrigin.BACKFILL));
                } else if (f.getStatus() != FindingStatus.OPEN) {
                    LocalDateTime changedAt = f.getResolvedAt() != null ? f.getResolvedAt() : detectedAt;
                    entries.add(FindingStatusHistory.of(f, FindingStatus.OPEN, f.getStatus(),
                            changedAt, null, null, HistoryOrigin.BACKFILL));
                }
            }
            historyRepository.saveAll(entries);
            log.info("Backfill de histórico concluído para {} finding(s) pré-existentes.", missingHistory.size());
        }

        List<Finding> missingScanLink = entityManager.createQuery(
                "SELECT f FROM Finding f WHERE f.scan IS NOT NULL AND NOT EXISTS ("
                        + "SELECT 1 FROM ScanFinding sf WHERE sf.scan = f.scan AND sf.finding = f)",
                Finding.class).getResultList();

        if (!missingScanLink.isEmpty()) {
            List<ScanFinding> links = new ArrayList<>();
            for (Finding f : missingScanLink) {
                links.add(new ScanFinding(f.getScan(), f, f.getDetectedAt() != null ? f.getDetectedAt() : LocalDateTime.now()));
            }
            scanFindingRepository.saveAll(links);
        }
    }
}
