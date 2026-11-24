package br.uva.tcc.sentry.report.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.uva.tcc.sentry.docs.api.ReportApi;
import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.DTO.ComplianceReportResponse;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse;
import br.uva.tcc.sentry.report.DTO.MttrReportResponse;
import br.uva.tcc.sentry.report.DTO.RelevantFindingsResponse;
import br.uva.tcc.sentry.report.DTO.ReportSummaryResponse;
import br.uva.tcc.sentry.report.DTO.RiskEvolutionResponse;
import br.uva.tcc.sentry.report.EvolutionGroupBy;
import br.uva.tcc.sentry.report.ReportFilter;
import br.uva.tcc.sentry.report.service.ComplianceReportService;
import br.uva.tcc.sentry.report.service.MttrReportService;
import br.uva.tcc.sentry.report.service.PdfReportService;
import br.uva.tcc.sentry.report.service.ReportService;
import br.uva.tcc.sentry.report.service.RiskEvolutionService;
import br.uva.tcc.sentry.shared.domain.Severity;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController implements ReportApi {

    private final ReportService reportService;
    private final RiskEvolutionService evolutionService;
    private final MttrReportService mttrService;
    private final ComplianceReportService complianceService;
    private final PdfReportService pdfService;

    @Override
    @GetMapping("/summary")
    public ReportSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID scanGroupId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) FindingType type,
            @RequestParam(required = false) FindingStatus status,
            @RequestParam(required = false) Long policyId) {
        return reportService.summary(filter(from, to, assetId, scanId, scanGroupId, severity, type, status, policyId));
    }

    @Override
    @GetMapping("/risk/evolution")
    public RiskEvolutionResponse riskEvolution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") EvolutionGroupBy groupBy,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID scanGroupId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) FindingType type) {
        return evolutionService.evolution(filter(from, to, assetId, scanId, scanGroupId, severity, type, null, null), groupBy);
    }

    @Override
    @GetMapping("/findings/distribution")
    public FindingsDistributionResponse distribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID scanGroupId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) FindingType type,
            @RequestParam(required = false) FindingStatus status) {
        return reportService.distribution(filter(from, to, assetId, scanId, scanGroupId, severity, type, status, null));
    }

    @Override
    @GetMapping("/findings/relevant")
    public RelevantFindingsResponse relevantFindings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "" + ReportService.DEFAULT_RELEVANT) int limit,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID scanGroupId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) FindingType type,
            @RequestParam(required = false) FindingStatus status) {
        return reportService.relevantFindings(filter(from, to, assetId, scanId, scanGroupId, severity, type, status, null), limit);
    }

    @Override
    @GetMapping("/mttr")
    public MttrReportResponse mttr(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID scanGroupId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) FindingType type) {
        return mttrService.report(filter(from, to, assetId, scanId, scanGroupId, severity, type, null, null));
    }

    @Override
    @GetMapping("/compliance")
    public ComplianceReportResponse compliance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long policyId,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID scanGroupId) {
        return complianceService.report(filter(from, to, assetId, scanId, scanGroupId, null, null, null, policyId));
    }

    @Override
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID scanId,
            @RequestParam(required = false) UUID scanGroupId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) FindingType type,
            @RequestParam(required = false) FindingStatus status,
            @RequestParam(required = false) Long policyId) {
        PdfReportService.GeneratedPdf pdf = pdfService.generate(
                filter(from, to, assetId, scanId, scanGroupId, severity, type, status, policyId));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(pdf.filename()).build().toString())
                .cacheControl(CacheControl.noStore())
                .contentLength(pdf.bytes().length)
                .body(pdf.bytes());
    }

    private static ReportFilter filter(LocalDate from, LocalDate to, UUID assetId, UUID scanId, UUID scanGroupId,
                                       Severity severity, FindingType type, FindingStatus status, Long policyId) {
        return ReportFilter.of(from, to, assetId, scanId, scanGroupId, severity, type, status, policyId, LocalDate.now());
    }
}
