package br.uva.tcc.sentry.scan.DTO;

import java.util.List;

public record ScanGroupDetailResponse(
    ScanGroupSummaryResponse group,
    List<ScanSummaryResponse> scans
) {
}
