package br.uva.tcc.sentry.finding.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingFingerprint;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.repository.FindingRepository;
import br.uva.tcc.sentry.scan.domain.Scan;
import br.uva.tcc.sentry.scan.domain.ScanCoverage;
import lombok.RequiredArgsConstructor;

/**
 * Concilia o que um scan encontrou com o que já está no banco, em vez de
 * simplesmente salvar tudo de novo:
 *
 * <ol>
 *   <li>cada candidato ganha um fingerprint; se o finding já existe, ele é
 *       atualizado (e eventualmente muda de status), senão é criado;</li>
 *   <li>findings ativos dos assets varridos que NÃO apareceram, e que o scan
 *       tinha cobertura para ver, são validados (corrigidos).</li>
 * </ol>
 *
 * As regras de transição de cada status ficam em {@link Finding}
 * ({@code onSeenAgain} / {@code onNotSeen}); aqui fica só a orquestração.
 * Roda numa única transação: ou o scan inteiro é reconciliado, ou nada.
 */
@Component
@RequiredArgsConstructor
public class FindingReconciler {

    private static final Logger log = LoggerFactory.getLogger(FindingReconciler.class);

    private final FindingRepository findingRepository;

    /** Resumo do que a reconciliação fez, para log e testes. */
    public record Result(int created, int refreshed, int reopened, int validationFailed, int validated) {
    }

    @Transactional
    public Result reconcile(Scan scan, List<Finding> candidates, ScanCoverage coverage) {
        return reconcile(scan, candidates, coverage, LocalDateTime.now());
    }

    /** Variante com o "agora" explícito (determinística para testes). */
    public Result reconcile(Scan scan, List<Finding> candidates, ScanCoverage coverage, LocalDateTime now) {
        LocalDateTime scanStartedAt = scan.getStartedAt();

        // 1. o que este scan observou, sem repetições (mesmo fingerprint = mesmo finding)
        Map<String, Finding> observed = new LinkedHashMap<>();
        for (Finding candidate : candidates) {
            candidate.setFingerprint(FindingFingerprint.of(candidate));
            observed.putIfAbsent(candidate.getFingerprint(), candidate);
        }

        // 2. o que já existia para essas mesmas identidades (qualquer status)
        Map<String, Finding> existing = new HashMap<>();
        if (!observed.isEmpty()) {
            for (Finding finding : findingRepository.findByFingerprintIn(observed.keySet())) {
                existing.put(finding.getFingerprint(), finding);
            }
        }

        int created = 0, refreshed = 0, reopened = 0, validationFailed = 0, validated = 0;
        List<Finding> toCreate = new ArrayList<>();

        for (Map.Entry<String, Finding> entry : observed.entrySet()) {
            Finding known = existing.get(entry.getKey());

            if (known == null) {
                Finding fresh = entry.getValue();
                fresh.setLastSeenAt(now);
                if (fresh.getDetectedAt() == null) {
                    fresh.setDetectedAt(now);
                }
                toCreate.add(fresh);
                created++;
                continue;
            }

            FindingStatus before = known.getStatus();
            known.onSeenAgain(entry.getValue(), scanStartedAt, now);
            FindingStatus after = known.getStatus();

            if (before == FindingStatus.VALIDATED && after == FindingStatus.OPEN) {
                reopened++;
            } else if (before != FindingStatus.FAILED_VALIDATION && after == FindingStatus.FAILED_VALIDATION) {
                validationFailed++;
            } else {
                refreshed++;
            }
        }

        // 3. o que existia, estava ativo e não apareceu — só vale se o scan tinha cobertura
        if (!coverage.scannedAssetIds().isEmpty()) {
            List<Finding> tracked = findingRepository.findTrackedByAssetIds(
                    coverage.scannedAssetIds(), FindingStatus.reconcilable());

            for (Finding finding : tracked) {
                if (observed.containsKey(finding.getFingerprint()) || !coverage.covers(finding)) {
                    continue;
                }

                finding.onNotSeen(scanStartedAt, now);

                if (finding.getStatus() == FindingStatus.VALIDATED) {
                    validated++;
                    log.info("Finding {} ({}) não foi reencontrado no scan {}: validado.",
                            finding.getId(), finding.getCode(), scan.getId());
                }
            }
        }

        findingRepository.saveAll(toCreate);

        return new Result(created, refreshed, reopened, validationFailed, validated);
    }
}
