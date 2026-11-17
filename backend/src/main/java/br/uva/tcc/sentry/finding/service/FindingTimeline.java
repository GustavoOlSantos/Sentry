package br.uva.tcc.sentry.finding.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.finding.domain.Finding;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingStatusHistory;
import br.uva.tcc.sentry.finding.domain.HistoryOrigin;
import br.uva.tcc.sentry.finding.domain.ScanFinding;
import br.uva.tcc.sentry.finding.repository.FindingStatusHistoryRepository;
import br.uva.tcc.sentry.finding.repository.ScanFindingRepository;
import br.uva.tcc.sentry.scan.domain.Scan;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FindingTimeline {

    private final FindingStatusHistoryRepository historyRepository;
    private final ScanFindingRepository scanFindingRepository;

    public void recordCreated(Finding finding, Scan scan, LocalDateTime at) {
        historyRepository.save(FindingStatusHistory.of(finding, null, finding.getStatus(), at, null, scan, HistoryOrigin.SCAN));
    }

    public void recordTransition(Finding finding, FindingStatus from, FindingStatus to, LocalDateTime at,
                                 Long userId, Scan scan) {
        if (from == to) {
            return;
        }
        HistoryOrigin origin = userId != null ? HistoryOrigin.USER : HistoryOrigin.SCAN;
        historyRepository.save(FindingStatusHistory.of(finding, from, to, at, userId, scan, origin));
    }

    public void replaceObservations(Scan scan, Collection<Finding> observed, LocalDateTime at) {
        scanFindingRepository.deleteByScanId(scan.getId());

        List<ScanFinding> rows = new ArrayList<>();
        for (Finding finding : observed) {
            rows.add(new ScanFinding(scan, finding, at));
        }
        scanFindingRepository.saveAll(rows);
    }
}
