package br.uva.tcc.sentry.report.pdf;

import static br.uva.tcc.sentry.report.pdf.PdfCharts.y;
import static br.uva.tcc.sentry.report.pdf.PdfDocument.PAGE_HEIGHT;
import static br.uva.tcc.sentry.report.pdf.PdfDocument.PAGE_WIDTH;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import br.uva.tcc.sentry.finding.domain.FindingStatus;
import br.uva.tcc.sentry.finding.domain.FindingType;
import br.uva.tcc.sentry.report.DTO.ComplianceReportResponse;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse.AssetFindings;
import br.uva.tcc.sentry.report.DTO.FindingsDistributionResponse.SeverityCount;
import br.uva.tcc.sentry.report.DTO.FullReport;
import br.uva.tcc.sentry.report.DTO.MttrReportResponse;
import br.uva.tcc.sentry.report.DTO.RelevantFindingsResponse.RelevantFinding;
import br.uva.tcc.sentry.report.DTO.ReportSummaryResponse;
import br.uva.tcc.sentry.report.DTO.RiskEvolutionResponse.RiskEvolutionPoint;
import br.uva.tcc.sentry.report.ReportSeverity;
import br.uva.tcc.sentry.report.pdf.PdfCharts.Plot;
import br.uva.tcc.sentry.report.pdf.PdfCharts.Series;
import br.uva.tcc.sentry.report.pdf.PdfDocument.Page;
import br.uva.tcc.sentry.report.pdf.PdfDocument.PdfImage;
import br.uva.tcc.sentry.report.pdf.PdfFonts.Font;

public final class ReportPdfRenderer {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DecimalFormat INTEGER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(PT_BR));
    private static final DecimalFormat DECIMAL = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(PT_BR));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final float M = 40;
    private static final float CW = PAGE_WIDTH - 2 * M;
    private static final float CONTENT_TOP = 62;
    private static final float CONTENT_BOTTOM = PAGE_HEIGHT - 54;

    private final FullReport report;
    private final PdfDocument doc;
    private final PdfImage logo;
    private final ZoneId zone;

    private Page page;
    private float top;

    private final List<String> tocTitles = new ArrayList<>();
    private final List<Integer> tocPages = new ArrayList<>();

    private ReportPdfRenderer(FullReport report, BufferedImage logoImage, boolean compress, ZoneId zone) {
        this.report = report;
        this.zone = zone;
        // os horários do relatório estão em UTC (sem zona); o instante real é esse mesmo valor em UTC
        this.doc = new PdfDocument("Relatório de Segurança - TC Sentry", "TC Sentry", compress,
                report.generatedAt().toInstant(br.uva.tcc.sentry.config.AppTime.STORAGE_ZONE));
        this.logo = logoImage != null ? doc.addImage(logoImage) : null;
    }

    public static byte[] render(FullReport report, BufferedImage logoImage, boolean compress) {
        return render(report, logoImage, compress, br.uva.tcc.sentry.config.AppTime.DEFAULT_DISPLAY_ZONE);
    }

    /** {@code zone} é o fuso em que as datas e horas aparecem no PDF (os dados chegam em UTC). */
    public static byte[] render(FullReport report, BufferedImage logoImage, boolean compress, ZoneId zone) {
        ReportPdfRenderer renderer = new ReportPdfRenderer(report, logoImage, compress, zone);
        renderer.build();
        return renderer.doc.toBytes();
    }

    /** Horário gravado (UTC) -> horário de parede no fuso de exibição. */
    private LocalDateTime local(LocalDateTime stored) {
        return br.uva.tcc.sentry.config.AppTime.toZone(stored, zone);
    }

    private void build() {
        cover();

        summarySection();
        evolutionSection();
        distributionSection();
        mttrSection();
        complianceSection();
        relevantSection();
        methodologySection();

        drawContents();
        drawFooters();
    }

    private void cover() {
        page = doc.addPage();

        page.fillColor(ReportPalette.NAVY).rect(0, y(250), PAGE_WIDTH, 250, true, false);
        page.fillColor(ReportPalette.PURPLE).rect(0, y(254), PAGE_WIDTH, 4, true, false);

        if (logo != null) {
            float width = 210;
            page.image(logo, M, y(56 + width / logo.aspectRatio()), width, width / logo.aspectRatio());
        } else {
            page.fillColor(ReportPalette.WHITE).text(M, y(80), Font.BOLD, 26, "TC Sentry");
        }

        page.fillColor(ReportPalette.WHITE).text(M, y(168), Font.BOLD, 30, "Relatório de Segurança");
        page.fillColor(0xB6C4DD).text(M, y(192), Font.REGULAR, 12, "Gestão de vulnerabilidades e compliance");
        page.fillColor(0x8FB2F5).text(M, y(224), Font.BOLD, 11,
                "Período: " + DATE.format(report.period().from()) + " a " + DATE.format(report.period().to()));

        float cardTop = 290;
        page.fillColor(ReportPalette.WHITE).strokeColor(ReportPalette.BORDER).lineWidth(0.8f)
                .roundRect(M, y(cardTop + 118), CW, 118, 6, true, true);

        infoRow(cardTop + 14, "Período analisado",
                DATE.format(report.period().from()) + " a " + DATE.format(report.period().to())
                        + " (" + report.period().days() + (report.period().days() == 1 ? " dia" : " dias") + ", datas inclusivas)");
        infoRow(cardTop + 40, "Escopo", report.scopeDescription());
        infoRow(cardTop + 66, "Gerado em", DATE_TIME.format(local(report.generatedAt())));
        infoRow(cardTop + 92, "Identificador", report.reportId());

        top = 450;
        page = null;
    }

    private void infoRow(float rowTop, String label, String value) {
        page.fillColor(ReportPalette.TEXT_MUTED).text(M + 16, y(rowTop + 8), Font.BOLD, 8, label.toUpperCase(Locale.ROOT));
        page.fillColor(ReportPalette.TEXT).text(M + 130, y(rowTop + 9), Font.REGULAR, 10,
                PdfFonts.fit(Font.REGULAR, value, 10, CW - 146));
    }

    private void drawContents() {
        Page cover = doc.pages().get(0);
        float cursor = 440;

        cover.fillColor(ReportPalette.TEXT).text(M, y(cursor + 12), Font.BOLD, 14, "Conteúdo");
        cover.fillColor(ReportPalette.PURPLE).rect(M, y(cursor + 22), 28, 2, true, false);
        cursor += 40;

        for (int i = 0; i < tocTitles.size(); i++) {
            String title = (i + 1) + ".  " + tocTitles.get(i);
            String pageNumber = String.valueOf(tocPages.get(i));

            cover.fillColor(ReportPalette.TEXT).text(M, y(cursor + 10), Font.REGULAR, 10.5f, title);
            cover.fillColor(ReportPalette.TEXT).textRight(M + CW, y(cursor + 10), Font.BOLD, 10.5f, pageNumber);

            float dotsFrom = M + PdfFonts.width(Font.REGULAR, title, 10.5f) + 6;
            float dotsTo = M + CW - PdfFonts.width(Font.BOLD, pageNumber, 10.5f) - 6;
            cover.strokeColor(ReportPalette.BORDER).lineWidth(0.8f).dashed(1, 3).line(dotsFrom, y(cursor + 12), dotsTo, y(cursor + 12)).solid();

            cursor += 24;
        }
    }

    private void newPage() {
        page = doc.addPage();

        page.fillColor(ReportPalette.NAVY).rect(0, y(34), PAGE_WIDTH, 34, true, false);
        page.fillColor(ReportPalette.PURPLE).rect(0, y(36), PAGE_WIDTH, 2, true, false);

        if (logo != null) {
            float height = 17;
            page.image(logo, M, y(8 + height), height * logo.aspectRatio(), height);
        }
        page.fillColor(0xB6C4DD).textRight(M + CW, y(21), Font.REGULAR, 8,
                "Relatório de Segurança  |  " + DATE.format(report.period().from()) + " a " + DATE.format(report.period().to()));

        top = CONTENT_TOP;
    }

    private void ensure(float height) {
        if (page == null || top + height > CONTENT_BOTTOM) {
            newPage();
        }
    }

    private void drawFooters() {
        int total = doc.pages().size();
        for (int i = 0; i < total; i++) {
            Page p = doc.pages().get(i);
            p.strokeColor(ReportPalette.BORDER).lineWidth(0.6f).line(M, y(PAGE_HEIGHT - 38), M + CW, y(PAGE_HEIGHT - 38));
            p.fillColor(ReportPalette.TEXT_MUTED).text(M, 24, Font.REGULAR, 7.5f,
                    "TC Sentry  |  " + report.reportId() + "  |  Gerado em " + DATE_TIME.format(local(report.generatedAt())));
            p.fillColor(ReportPalette.TEXT_MUTED).textRight(M + CW, 24, Font.REGULAR, 7.5f, "Página " + (i + 1) + " de " + total);
        }
    }

    private void section(String title) {
        if (page == null || top > CONTENT_BOTTOM - 150) {
            newPage();
        }

        tocTitles.add(title);
        tocPages.add(doc.pages().size());

        int number = tocTitles.size();
        top += 6;

        page.fillColor(ReportPalette.BLUE).roundRect(M, y(top + 20), 4, 20, 2, true, false);
        page.fillColor(ReportPalette.TEXT).text(M + 12, y(top + 15), Font.BOLD, 15, number + ". " + title);
        page.strokeColor(ReportPalette.BORDER).lineWidth(0.6f).line(M, y(top + 26), M + CW, y(top + 26));
        top += 38;
    }

    private void subtitle(String text) {
        ensure(30);
        page.fillColor(ReportPalette.TEXT).text(M, y(top + 11), Font.BOLD, 10.5f, text);
        top += 18;
    }

    private void paragraph(String text, float size, int color) {
        paragraph(text, size, color, Font.REGULAR, M, CW);
    }

    private void paragraph(String text, float size, int color, Font font, float x, float width) {
        float lineHeight = size * 1.45f;
        for (String line : wrap(text, font, size, width)) {
            ensure(lineHeight + 2);
            page.fillColor(color).text(x, y(top + size), font, size, line);
            top += lineHeight;
        }
        top += 4;
    }

    private void bullets(List<String> items, float size) {
        for (String item : items) {
            List<String> lines = wrap(item, Font.REGULAR, size, CW - 14);
            for (int i = 0; i < lines.size(); i++) {
                ensure(size * 1.45f + 2);
                if (i == 0) {
                    page.fillColor(ReportPalette.BLUE).text(M + 2, y(top + size), Font.BOLD, size, "\u2022");
                }
                page.fillColor(ReportPalette.TEXT).text(M + 14, y(top + size), Font.REGULAR, size, lines.get(i));
                top += size * 1.45f;
            }
            top += 2;
        }
        top += 2;
    }

    private static List<String> wrap(String text, Font font, float size, float maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : PdfFonts.sanitize(text).split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (PdfFonts.width(font, candidate, size) <= maxWidth || current.isEmpty()) {
                current = new StringBuilder(candidate);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private record Kpi(String value, String label, String note, int accent) {
    }

    private void kpiRow(List<Kpi> items) {
        float gap = 10;
        int columns = Math.min(4, items.size());
        float width = (CW - gap * (columns - 1)) / columns;
        float height = 64;

        ensure(height + 6);
        for (int i = 0; i < items.size(); i++) {
            float x = M + i * (width + gap);

            page.fillColor(ReportPalette.WHITE).strokeColor(ReportPalette.BORDER).lineWidth(0.8f)
                    .roundRect(x, y(top + height), width, height, 5, true, true);
            page.fillColor(items.get(i).accent()).roundRect(x, y(top + height), 4, height, 2, true, false);

            page.fillColor(ReportPalette.TEXT).text(x + 14, y(top + 27), Font.BOLD, 19,
                    PdfFonts.fit(Font.BOLD, items.get(i).value(), 19, width - 22));
            page.fillColor(ReportPalette.TEXT).text(x + 14, y(top + 42), Font.BOLD, 8,
                    PdfFonts.fit(Font.BOLD, items.get(i).label(), 8, width - 22));
            page.fillColor(ReportPalette.TEXT_MUTED).text(x + 14, y(top + 54), Font.REGULAR, 7.5f,
                    PdfFonts.fit(Font.REGULAR, items.get(i).note(), 7.5f, width - 22));
        }
        top += height + 10;
    }

    private record Col(String title, float weight, boolean right) {
        static Col left(String title, float weight) {
            return new Col(title, weight, false);
        }

        static Col right(String title, float weight) {
            return new Col(title, weight, true);
        }
    }

    private record Cell(String text, Integer badge, Integer color) {
        static Cell of(String text) {
            return new Cell(text, null, null);
        }

        static Cell colored(String text, int color) {
            return new Cell(text, null, color);
        }

        static Cell badge(String text, int color) {
            return new Cell(text, color, null);
        }
    }

    private void table(List<Col> columns, List<List<Cell>> rows) {
        float totalWeight = 0;
        for (Col column : columns) {
            totalWeight += column.weight();
        }
        float[] widths = new float[columns.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = CW * columns.get(i).weight() / totalWeight;
        }

        float headerHeight = 20;
        float rowHeight = 19;

        ensure(headerHeight + rowHeight * Math.min(2, Math.max(1, rows.size())));
        tableHeader(columns, widths, headerHeight);

        for (int r = 0; r < rows.size(); r++) {
            if (top + rowHeight > CONTENT_BOTTOM) {
                newPage();
                tableHeader(columns, widths, headerHeight);
            }

            if (r % 2 == 1) {
                page.fillColor(ReportPalette.ZEBRA).rect(M, y(top + rowHeight), CW, rowHeight, true, false);
            }

            float x = M;
            for (int c = 0; c < columns.size(); c++) {
                tableCell(rows.get(r).get(c), x, widths[c], columns.get(c).right());
                x += widths[c];
            }

            page.strokeColor(ReportPalette.GRID).lineWidth(0.5f).line(M, y(top + rowHeight), M + CW, y(top + rowHeight));
            top += rowHeight;
        }
        top += 10;
    }

    private void tableHeader(List<Col> columns, float[] widths, float height) {
        page.fillColor(ReportPalette.BLUE_LIGHT).rect(M, y(top + height), CW, height, true, false);

        float x = M;
        for (int c = 0; c < columns.size(); c++) {
            float inner = widths[c] - 12;
            String title = PdfFonts.fit(Font.BOLD, columns.get(c).title(), 7.5f, inner);
            if (columns.get(c).right()) {
                page.fillColor(ReportPalette.NAVY).textRight(x + widths[c] - 6, y(top + 13), Font.BOLD, 7.5f, title);
            } else {
                page.fillColor(ReportPalette.NAVY).text(x + 6, y(top + 13), Font.BOLD, 7.5f, title);
            }
            x += widths[c];
        }
        top += height;
    }

    private void tableCell(Cell cell, float x, float width, boolean right) {
        float inner = width - 12;

        if (cell.badge() != null) {
            String text = PdfFonts.fit(Font.BOLD, cell.text(), 7, inner - 8);
            float badgeWidth = PdfFonts.width(Font.BOLD, text, 7) + 10;
            float badgeX = right ? x + width - 6 - badgeWidth : x + 6;
            page.fillColor(cell.badge()).roundRect(badgeX, y(top + 15), badgeWidth, 12, 6, true, false);
            page.fillColor(ReportPalette.WHITE).text(badgeX + 5, y(top + 12.2f), Font.BOLD, 7, text);
            return;
        }

        String text = PdfFonts.fit(Font.REGULAR, cell.text(), 8, inner);
        page.fillColor(cell.color() != null ? cell.color() : ReportPalette.TEXT);
        if (right) {
            page.textRight(x + width - 6, y(top + 12.5f), Font.REGULAR, 8, text);
        } else {
            page.text(x + 6, y(top + 12.5f), Font.REGULAR, 8, text);
        }
    }

    private void summarySection() {
        ReportSummaryResponse s = report.summary();
        section("Resumo executivo");

        List<Kpi> first = List.of(
                new Kpi(num(s.totalFindings()), "Total de findings",
                        num(s.detectedInPeriod()) + " no período | " + num(s.carriedOver()) + " anteriores", ReportPalette.BLUE),
                new Kpi(num(s.open()), "Findings abertos", num(s.inProgress()) + " em andamento", ReportPalette.severity(ReportSeverity.CRITICAL)),
                new Kpi(num(s.critical()), "Findings críticos", num(s.pendingCritical()) + " ainda pendentes", ReportPalette.severity(ReportSeverity.CRITICAL)),
                new Kpi(num(s.high()), "Alta severidade", num(s.pendingHigh()) + " ainda pendentes", ReportPalette.severity(ReportSeverity.HIGH)));
        kpiRow(first);

        String mttr = s.mttr().averageSeconds() == null ? "Sem dados" : s.mttr().averageLabel();
        String mttrNote = s.mttr().averageSeconds() == null
                ? "nenhum finding validado no período" : num(s.mttr().completedFindings()) + " findings concluídos";
        String compliance = s.compliance().percentage() == null ? "Sem dados" : pct(s.compliance().percentage());
        String complianceNote = s.compliance().percentage() == null
                ? "sem avaliações de política" : num(s.compliance().evaluations()) + " avaliações";

        List<Kpi> second = List.of(
                new Kpi(num(s.resolvedOrValidated()), "Resolvidos ou validados", num(s.concludedInPeriod()) + " concluídos no período", ReportPalette.GREEN),
                new Kpi(mttr, "MTTR médio", mttrNote, ReportPalette.BLUE),
                new Kpi(compliance, "Compliance", complianceNote, ReportPalette.PURPLE),
                new Kpi(num(s.assetsAnalyzed()), "Ativos analisados", num(s.assetsWithFindings()) + " com findings", ReportPalette.BLUE));
        kpiRow(second);

        paragraph("No período de " + DATE.format(report.period().from()) + " a " + DATE.format(report.period().to())
                + " (" + report.period().days() + " dias), o escopo reúne " + num(s.totalFindings()) + " findings: "
                + num(s.detectedInPeriod()) + " detectados no período e " + num(s.carriedOver())
                + " anteriores que continuaram ativos. Hoje, " + num(s.open()) + " estão abertos, " + num(s.inProgress())
                + " em andamento, " + num(s.resolved()) + " resolvidos aguardando validação e " + num(s.validated())
                + " validados. " + num(s.acceptedRisk()) + (s.acceptedRisk() == 1 ? " é um risco aceito" : " são riscos aceitos")
                + " (justificados), que não é considerado corrigido.", 9.5f, ReportPalette.TEXT);

        paragraph("Cada finding é contado por ocorrência (um mesmo CVE em ativos ou serviços diferentes gera ocorrências distintas). "
                + "A quantidade de findings, sozinha, não mede risco: a severidade e o estado de cada ocorrência são considerados nos indicadores acima.",
                8.5f, ReportPalette.TEXT_MUTED);
    }

    private void evolutionSection() {
        section("Evolução do risco");

        paragraph("Os gráficos mostram, dia a dia, o que foi detectado, o que foi concluído (validado) e o que permanecia pendente ao fim do dia, "
                + "reconstruído a partir do histórico de transições de status. Marcar um finding como resolvido não o conclui: só a validação conclui.",
                9, ReportPalette.TEXT);

        List<RiskEvolutionPoint> points = report.evolution().points();
        List<String> labels = points.stream().map(point -> SHORT_DATE.format(point.date())).toList();

        subtitle("Findings detectados e concluídos por dia");
        float chartHeight = 118;
        ensure(chartHeight + 44);

        PdfCharts.legend(page, M + 34, top, List.of("Detectados no dia", "Concluídos (validados) no dia"),
                new int[] { ReportPalette.BLUE, ReportPalette.GREEN });
        top += 16;

        Plot plotA = new Plot(M + 34, top, CW - 42, chartHeight);
        boolean anyEvent = points.stream().anyMatch(p -> p.detected() > 0 || p.resolved() > 0);
        if (points.isEmpty() || !anyEvent) {
            PdfCharts.emptyState(page, plotA, "Nenhum finding detectado ou concluído no período.");
        } else {
            PdfCharts.lineChart(page, plotA, labels, List.of(
                    new Series("Detectados", ReportPalette.BLUE, points.stream().mapToDouble(RiskEvolutionPoint::detected).toArray()),
                    new Series("Concluídos", ReportPalette.GREEN, points.stream().mapToDouble(RiskEvolutionPoint::resolved).toArray())), false);
        }
        top += chartHeight + 28;

        subtitle("Findings pendentes ao fim do dia, por severidade");
        ensure(chartHeight + 44);

        List<ReportSeverity> order = ReportSeverity.ORDERED;
        List<String> legendLabels = new ArrayList<>();
        int[] legendColors = new int[order.size() + 1];
        for (int i = 0; i < order.size(); i++) {
            legendLabels.add(severityName(order.get(i)));
            legendColors[i] = ReportPalette.severity(order.get(i));
        }
        legendLabels.add("Risco aceito (justificado)");
        legendColors[order.size()] = ReportPalette.PURPLE;

        float legendEnd = PdfCharts.legend(page, M + 34, top, legendLabels, legendColors);
        top += legendEnd > CW - 40 ? 28 : 16;

        Plot plotB = new Plot(M + 34, top, CW - 42, chartHeight);
        boolean anyPending = points.stream().anyMatch(p -> p.pending() > 0 || p.acceptedRisk() > 0);
        if (points.isEmpty() || !anyPending) {
            PdfCharts.emptyState(page, plotB, "Nenhum finding pendente no período.");
        } else {
            List<Series> stack = new ArrayList<>();
            for (ReportSeverity severity : order) {
                stack.add(new Series(severityName(severity), ReportPalette.severity(severity),
                        points.stream().mapToDouble(p -> p.bySeverity().getOrDefault(severity, 0L)).toArray()));
            }
            Series accepted = new Series("Risco aceito", ReportPalette.PURPLE,
                    points.stream().mapToDouble(RiskEvolutionPoint::acceptedRisk).toArray());
            PdfCharts.stackedAreaChart(page, plotB, labels, stack, accepted);
        }
        top += chartHeight + 28;

        paragraph("A linha tracejada roxa mostra os riscos aceitos por justificativa: eles saem do total de pendentes, mas continuam existindo e "
                + "nunca são contados como corrigidos.", 8.5f, ReportPalette.TEXT_MUTED);

        for (String note : report.evolution().limitations()) {
            paragraph("Observação: " + note, 8, ReportPalette.TEXT_MUTED, Font.ITALIC, M, CW);
        }
    }

    private void distributionSection() {
        FindingsDistributionResponse d = report.distribution();
        section("Distribuição de findings");

        paragraph("Total de " + num(d.total()) + " findings no escopo. Os percentuais consideram o total do filtro aplicado.", 9, ReportPalette.TEXT);

        float blockHeight = 188;
        ensure(blockHeight);
        float blockTop = top;

        double[] severityValues = d.bySeverity().stream().mapToDouble(SeverityCount::count).toArray();
        int[] severityColors = d.bySeverity().stream().mapToInt(c -> ReportPalette.severity(c.severity())).toArray();
        float leftCenter = M + CW * 0.25f;
        page.fillColor(ReportPalette.TEXT).textCenter(leftCenter, y(blockTop + 10), Font.BOLD, 9.5f, "Por severidade");
        PdfCharts.donut(page, leftCenter, blockTop + 62, 46, 28, severityValues, severityColors, num(d.total()), "findings");
        for (int i = 0; i < d.bySeverity().size(); i++) {
            SeverityCount c = d.bySeverity().get(i);
            legendRow(M + CW * 0.25f - 80, blockTop + 120 + i * 13, ReportPalette.severity(c.severity()),
                    severityName(c.severity()), num(c.count()) + " (" + pct(c.percentage()) + ")", 160);
        }

        double[] typeValues = d.byType().stream().mapToDouble(c -> c.count()).toArray();
        int[] typeColors = d.byType().stream().mapToInt(c -> c.type() == FindingType.VULNERABILITY ? ReportPalette.BLUE : ReportPalette.PURPLE).toArray();
        float rightCenter = M + CW * 0.75f;
        page.fillColor(ReportPalette.TEXT).textCenter(rightCenter, y(blockTop + 10), Font.BOLD, 9.5f, "Por tipo");
        PdfCharts.donut(page, rightCenter, blockTop + 62, 46, 28, typeValues, typeColors, num(d.total()), "findings");
        for (int i = 0; i < d.byType().size(); i++) {
            var c = d.byType().get(i);
            legendRow(rightCenter - 80, blockTop + 120 + i * 13, typeColors[i], typeName(c.type()),
                    num(c.count()) + " (" + pct(c.percentage()) + ")", 160);
        }
        top = blockTop + blockHeight;

        subtitle("Por status (e coluna do Kanban em que aparece)");
        List<List<Cell>> statusRows = new ArrayList<>();
        for (var c : d.byStatus()) {
            statusRows.add(List.of(Cell.badge(statusName(c.status()), ReportPalette.status(c.status())),
                    Cell.of(columnName(c.column().name())), Cell.of(num(c.count())), Cell.of(pct(c.percentage()))));
        }
        table(List.of(Col.left("Status", 3), Col.left("Coluna do Kanban", 3), Col.right("Findings", 2), Col.right("%", 2)), statusRows);

        subtitle("Ativos com maior concentração de findings");
        if (d.topAssets().isEmpty()) {
            paragraph("Nenhum ativo com findings no escopo.", 9, ReportPalette.TEXT_MUTED, Font.ITALIC, M, CW);
        } else {
            List<List<Cell>> assetRows = new ArrayList<>();
            for (AssetFindings a : d.topAssets()) {
                String name = a.hostname() == null || a.hostname().isBlank() ? a.ipAddress() : a.ipAddress() + " (" + a.hostname() + ")";
                assetRows.add(List.of(Cell.of(name), Cell.of(num(a.total())),
                        Cell.colored(num(a.bySeverity().getOrDefault(ReportSeverity.CRITICAL, 0L)), ReportPalette.severity(ReportSeverity.CRITICAL)),
                        Cell.colored(num(a.bySeverity().getOrDefault(ReportSeverity.HIGH, 0L)), ReportPalette.severity(ReportSeverity.HIGH)),
                        Cell.of(num(a.bySeverity().getOrDefault(ReportSeverity.MEDIUM, 0L))),
                        Cell.of(num(a.bySeverity().getOrDefault(ReportSeverity.LOW, 0L))),
                        Cell.of(num(a.pending())), Cell.of(num(a.validated())), Cell.of(num(a.acceptedRisk()))));
            }
            table(List.of(Col.left("Ativo", 5), Col.right("Total", 1.2f), Col.right("Crít.", 1.2f), Col.right("Alta", 1.2f),
                    Col.right("Média", 1.2f), Col.right("Baixa", 1.2f), Col.right("Pendentes", 1.6f), Col.right("Validados", 1.6f),
                    Col.right("Aceitos", 1.4f)), assetRows);
        }
    }

    private void legendRow(float x, float rowTop, int color, String label, String value, float width) {
        page.fillColor(color).roundRect(x, y(rowTop + 8), 8, 8, 1.5f, true, false);
        page.fillColor(ReportPalette.TEXT).text(x + 13, y(rowTop + 7), Font.REGULAR, 8, label);
        page.fillColor(ReportPalette.TEXT).textRight(x + width, y(rowTop + 7), Font.BOLD, 8, value);
    }

    private void mttrSection() {
        MttrReportResponse m = report.mttr();
        section("MTTR (tempo médio de resolução)");

        paragraph("MTTR = data de validação - data de detecção, dos findings validados no período. Findings abertos, em andamento, com falha de validação "
                + "ou apenas justificados não entram no cálculo.", 9, ReportPalette.TEXT);

        if (m.completedFindings() == 0) {
            kpiRow(List.of(new Kpi("Sem dados", "MTTR médio", "nenhum finding validado no período", ReportPalette.GRAY),
                    new Kpi(num(m.pendingFindings()), "Findings pendentes", "não considerados no cálculo", ReportPalette.severity(ReportSeverity.HIGH))));
            paragraph("Sem dados suficientes: nenhum finding foi validado no período e escopo selecionados.", 9, ReportPalette.TEXT_MUTED, Font.ITALIC, M, CW);
            return;
        }

        kpiRow(List.of(
                new Kpi(m.averageLabel(), "MTTR médio", DECIMAL.format(m.averageHours()) + " horas", ReportPalette.BLUE),
                new Kpi(m.medianLabel(), "Mediana", "menos sensível a casos extremos", ReportPalette.BLUE),
                new Kpi(m.minLabel(), "Menor tempo", "validação mais rápida", ReportPalette.GREEN),
                new Kpi(m.maxLabel(), "Maior tempo", "validação mais demorada", ReportPalette.severity(ReportSeverity.HIGH))));

        paragraph(num(m.completedFindings()) + " findings concluídos considerados no cálculo; " + num(m.pendingFindings())
                + " ainda pendentes (não considerados).", 9, ReportPalette.TEXT);

        subtitle("MTTR médio por severidade");
        List<String> labels = new ArrayList<>();
        List<String> valueLabels = new ArrayList<>();
        List<Double> hours = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (var row : m.bySeverity()) {
            labels.add(severityName(row.severity()) + " (" + num(row.completedCount()) + ")");
            valueLabels.add(row.averageSeconds() == null ? "sem dados" : row.averageLabel());
            hours.add(row.averageSeconds() == null ? 0.0 : row.averageSeconds() / 3600.0);
            colors.add(ReportPalette.severity(row.severity()));
        }
        ensure(labels.size() * 20 + 10);
        float used = PdfCharts.horizontalBars(page, M, top, CW, 120, labels, hours.stream().mapToDouble(Double::doubleValue).toArray(),
                colors.stream().mapToInt(Integer::intValue).toArray(), valueLabels);
        top += used + 8;

        subtitle("MTTR médio por tipo");
        List<List<Cell>> typeRows = new ArrayList<>();
        for (var row : m.byType()) {
            typeRows.add(List.of(Cell.of(typeName(row.type())), Cell.of(num(row.completedCount())),
                    Cell.of(row.averageSeconds() == null ? "sem dados" : row.averageLabel())));
        }
        table(List.of(Col.left("Tipo", 4), Col.right("Findings concluídos", 2), Col.right("MTTR médio", 2)), typeRows);
    }

    private void complianceSection() {
        ComplianceReportResponse c = report.compliance();
        section("Compliance e políticas");

        paragraph("Compliance (%) = avaliações conformes / (conformes + não conformes). Considera a última avaliação de cada par ativo-regra no período; "
                + "avaliações não aplicáveis e com erro não entram no denominador.", 9, ReportPalette.TEXT);

        if (!c.evaluationsAvailable()) {
            ensure(60);
            page.fillColor(ReportPalette.PURPLE_LIGHT).roundRect(M, y(top + 46), CW, 46, 5, true, false);
            page.fillColor(ReportPalette.TEXT).text(M + 14, y(top + 19), Font.BOLD, 9.5f, "O indicador de compliance não pôde ser calculado.");
            page.fillColor(ReportPalette.TEXT_MUTED).text(M + 14, y(top + 34), Font.REGULAR, 8.5f,
                    "Nenhuma avaliação de política foi registrada no período para os filtros informados.");
            top += 58;
            return;
        }

        kpiRow(List.of(
                new Kpi(c.compliancePercentage() == null ? "Sem dados" : pct(c.compliancePercentage()), "Compliance",
                        c.compliancePercentage() == null ? "nenhuma avaliação aplicável" : "sobre avaliações aplicáveis", ReportPalette.PURPLE),
                new Kpi(num(c.totalEvaluations()), "Avaliações", "ativo x regra (última)", ReportPalette.BLUE),
                new Kpi(num(c.compliant()), "Conformes", "regra cumprida", ReportPalette.GREEN),
                new Kpi(num(c.nonCompliant()), "Não conformes", "violações ativas", ReportPalette.severity(ReportSeverity.CRITICAL))));
        paragraph(num(c.notApplicable()) + " avaliações não aplicáveis e " + num(c.errors())
                + " com erro (regra mal configurada ou falha técnica) ficaram fora do percentual.", 8.5f, ReportPalette.TEXT_MUTED);

        ensure(34);
        PdfCharts.stackedBar(page, M, top, CW, 14,
                new double[] { c.compliant(), c.nonCompliant(), c.notApplicable(), c.errors() },
                new int[] { ReportPalette.GREEN, ReportPalette.severity(ReportSeverity.CRITICAL), ReportPalette.GRAY, ReportPalette.severity(ReportSeverity.MEDIUM) });
        top += 20;
        PdfCharts.legend(page, M, top, List.of("Conformes", "Não conformes", "Não aplicáveis", "Com erro"),
                new int[] { ReportPalette.GREEN, ReportPalette.severity(ReportSeverity.CRITICAL), ReportPalette.GRAY, ReportPalette.severity(ReportSeverity.MEDIUM) });
        top += 22;

        subtitle("Conformidade por política");
        List<List<Cell>> policyRows = new ArrayList<>();
        for (var p : c.byPolicy()) {
            policyRows.add(List.of(Cell.of(p.policyName()), Cell.of(num(p.compliant())), Cell.of(num(p.nonCompliant())),
                    Cell.of(num(p.notApplicable())), Cell.of(num(p.errors())),
                    Cell.colored(p.compliancePercentage() == null ? "sem dados" : pct(p.compliancePercentage()), ReportPalette.BLUE)));
        }
        table(List.of(Col.left("Política", 5), Col.right("Conformes", 1.6f), Col.right("Não conf.", 1.6f), Col.right("N/A", 1.2f),
                Col.right("Erros", 1.2f), Col.right("Compliance", 1.8f)), policyRows);

        subtitle("Principais violações encontradas");
        if (c.topViolations().isEmpty()) {
            paragraph("Nenhuma regra violada na última avaliação.", 9, ReportPalette.TEXT_MUTED, Font.ITALIC, M, CW);
        } else {
            List<List<Cell>> violationRows = new ArrayList<>();
            for (var v : c.topViolations()) {
                violationRows.add(List.of(Cell.of(v.ruleName()), Cell.of(v.policyName()),
                        Cell.badge(severityName(v.severity()), ReportPalette.severity(v.severity())), Cell.of(num(v.affectedAssets()))));
            }
            table(List.of(Col.left("Regra", 4), Col.left("Política", 3), Col.left("Severidade", 2), Col.right("Ativos afetados", 2)), violationRows);
        }

        subtitle("Conformidade por ativo");
        List<List<Cell>> assetRows = new ArrayList<>();
        int shown = 0;
        for (var a : c.byAsset()) {
            if (shown++ >= 10) {
                break;
            }
            String name = a.hostname() == null || a.hostname().isBlank() ? a.ipAddress() : a.ipAddress() + " (" + a.hostname() + ")";
            assetRows.add(List.of(Cell.of(name), Cell.of(num(a.compliant())), Cell.of(num(a.nonCompliant())),
                    Cell.of(num(a.notApplicable())), Cell.of(num(a.errors())),
                    Cell.colored(a.compliancePercentage() == null ? "sem dados" : pct(a.compliancePercentage()), ReportPalette.BLUE)));
        }
        table(List.of(Col.left("Ativo", 5), Col.right("Conformes", 1.6f), Col.right("Não conf.", 1.6f), Col.right("N/A", 1.2f),
                Col.right("Erros", 1.2f), Col.right("Compliance", 1.8f)), assetRows);
        if (c.byAsset().size() > 10) {
            paragraph("Exibindo os 10 ativos com mais violações de " + c.byAsset().size() + ".", 8, ReportPalette.TEXT_MUTED, Font.ITALIC, M, CW);
        }
    }

    private void relevantSection() {
        var rel = report.relevantFindings();
        section("Findings relevantes");

        if (rel.findings().isEmpty()) {
            paragraph("Nenhum finding no escopo selecionado.", 9, ReportPalette.TEXT_MUTED, Font.ITALIC, M, CW);
            return;
        }

        paragraph("Seleção dos " + num(rel.findings().size()) + " findings de maior prioridade, de um total de " + num(rel.totalInScope())
                + " no escopo. A ordem é por severidade (crítica, alta, média, baixa), com os ainda não concluídos antes dos fechados e, "
                + "em seguida, os mais recentes.", 9, ReportPalette.TEXT);

        List<List<Cell>> rows = new ArrayList<>();
        for (RelevantFinding f : rel.findings()) {
            String asset = f.assetIp() + (f.port() != null ? ":" + f.port() : "");
            rows.add(List.of(Cell.of(f.title() != null && !f.title().isBlank() ? f.title() : f.code()),
                    Cell.badge(severityName(f.severity()), ReportPalette.severity(f.severity())),
                    Cell.of(typeName(f.type())),
                    Cell.colored(statusName(f.status()), ReportPalette.status(f.status())),
                    Cell.of(asset),
                    Cell.of(f.detectedAt() == null ? "-" : DATE.format(local(f.detectedAt())))));
        }
        table(List.of(Col.left("Título", 6), Col.left("Severidade", 2), Col.left("Tipo", 2.4f), Col.left("Status", 2.4f),
                Col.left("Ativo", 2.6f), Col.left("Detecção", 2)), rows);
    }

    private void methodologySection() {
        section("Metodologia e observações");

        subtitle("Critérios de cálculo");
        bullets(List.of(
                "Período: datas inicial e final inclusivas, no mesmo fuso em que o sistema grava os registros. Sem período informado, usa-se os últimos 30 dias.",
                "Escopo do período: findings detectados até o fim do período e não validados antes do começo dele (os que estiveram ativos em algum momento do período).",
                "Contagem: cada ocorrência (finding) é contada uma vez, independentemente do CVE. Os filtros se combinam (todos precisam ser atendidos).",
                "Detectados no período: data de detecção dentro do período. Concluídos: data de validação dentro do período."), 8.5f);

        subtitle("Severidades e status");
        bullets(List.of(
                "Severidades: CRITICAL, HIGH, MEDIUM e LOW; INFO reúne findings informativos ou sem classificação.",
                "Pendentes: abertos, em andamento, com falha de validação ou resolvidos ainda não validados. A conclusão efetiva de um finding é a validação.",
                "Kanban: ABERTO (aberto); EM ANDAMENTO (em andamento e falha de validação); RESOLVIDA (resolvido); FECHADA (justificado e validado).",
                "Justificado (JUSTIFIED) é risco aceito: é exibido à parte e nunca considerado corrigido."), 8.5f);

        subtitle("MTTR");
        bullets(List.of(
                "MTTR (Mean Time To Resolution) = data de validação - data de detecção, em média, dos findings validados no período.",
                "Não entram: findings abertos, em andamento, com falha de validação, apenas justificados ou sem datas válidas. A mediana reduz a influência de casos extremos."), 8.5f);

        subtitle("Compliance");
        bullets(List.of(
                "Compliance (%) = avaliações conformes / (conformes + não conformes) x 100, sobre a última avaliação de cada par ativo-regra.",
                "Regras desabilitadas, avaliações não aplicáveis e avaliações com erro técnico não entram no denominador. A ausência de um finding não é tratada como conformidade."), 8.5f);

        Set<String> limitations = new LinkedHashSet<>(report.summary().limitations());
        limitations.addAll(report.evolution().limitations());
        subtitle("Limitações dos dados");
        bullets(new ArrayList<>(limitations), 8.5f);
    }

    private static String num(long value) {
        return INTEGER.format(value);
    }

    private static String pct(double value) {
        return DECIMAL.format(value) + "%";
    }

    private static String severityName(ReportSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "Crítica";
            case HIGH -> "Alta";
            case MEDIUM -> "Média";
            case LOW -> "Baixa";
            case INFO -> "Informativa";
        };
    }

    private static String typeName(FindingType type) {
        return type == FindingType.VULNERABILITY ? "Vulnerabilidade" : "Violação de política";
    }

    private static String statusName(FindingStatus status) {
        return switch (status) {
            case OPEN -> "Aberto";
            case IN_PROGRESS -> "Em andamento";
            case FAILED_VALIDATION -> "Reteste falhou";
            case RESOLVED -> "Resolvido";
            case JUSTIFIED -> "Justificado";
            case VALIDATED -> "Validado";
        };
    }

    private static String columnName(String column) {
        return switch (column) {
            case "ABERTO" -> "Aberto";
            case "EM_ANDAMENTO" -> "Em andamento";
            case "RESOLVIDA" -> "Resolvida";
            default -> "Fechada";
        };
    }
}
