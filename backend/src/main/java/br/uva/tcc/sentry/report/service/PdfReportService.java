package br.uva.tcc.sentry.report.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.uva.tcc.sentry.report.DTO.FullReport;
import br.uva.tcc.sentry.report.DTO.Period;
import br.uva.tcc.sentry.report.ReportFilter;
import br.uva.tcc.sentry.report.pdf.ReportPdfRenderer;

@Service
public class PdfReportService {

    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);
    private static final String LOGO_RESOURCE = "/report/logo.png";

    private final ReportService reportService;
    private final BufferedImage logo;

    public record GeneratedPdf(String filename, byte[] bytes) {
    }

    public PdfReportService(ReportService reportService) {
        this.reportService = reportService;
        this.logo = loadLogo();
    }

    public GeneratedPdf generate(ReportFilter filter) {
        return render(reportService.fullReport(filter));
    }

    public GeneratedPdf render(FullReport report) {
        return new GeneratedPdf(filename(report.period()), ReportPdfRenderer.render(report, logo, true));
    }

    public static String filename(Period period) {
        return "sentry-report-" + period.from() + "-to-" + period.to() + ".pdf";
    }

    private static BufferedImage loadLogo() {
        try (InputStream stream = PdfReportService.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (stream == null) {
                log.warn("Logo {} não encontrado: o PDF será gerado só com o nome do produto.", LOGO_RESOURCE);
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            log.warn("Não foi possível ler o logo do relatório: {}", e.getMessage());
            return null;
        }
    }
}
