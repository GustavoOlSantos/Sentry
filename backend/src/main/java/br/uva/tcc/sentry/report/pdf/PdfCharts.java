package br.uva.tcc.sentry.report.pdf;

import static br.uva.tcc.sentry.report.pdf.PdfDocument.PAGE_HEIGHT;

import java.util.List;

import br.uva.tcc.sentry.report.pdf.PdfDocument.Page;
import br.uva.tcc.sentry.report.pdf.PdfFonts.Font;

final class PdfCharts {

    private PdfCharts() {
    }

    static float y(float top) {
        return PAGE_HEIGHT - top;
    }

    record Series(String name, int color, double[] values) {
    }

    static void donut(Page page, float cx, float cyTop, float outerR, float innerR,
                      double[] values, int[] colors, String centerValue, String centerLabel) {
        double total = 0;
        for (double v : values) {
            total += v;
        }

        float cy = y(cyTop);

        if (total <= 0) {
            ring(page, cx, cy, outerR, innerR, 0, 360, ReportPalette.GRID);
        } else {
            double start = 90;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) {
                    continue;
                }
                double sweep = values[i] / total * 360;
                double gap = countPositive(values) > 1 ? 0.6 : 0;
                ring(page, cx, cy, outerR, innerR, start - sweep + gap / 2, start - gap / 2, colors[i]);
                start -= sweep;
            }
        }

        page.fillColor(ReportPalette.TEXT).textCenter(cx, cy - 2, Font.BOLD, 17, centerValue);
        page.fillColor(ReportPalette.TEXT_MUTED).textCenter(cx, cy - 14, Font.REGULAR, 7.5f, centerLabel);
    }

    private static int countPositive(double[] values) {
        int n = 0;
        for (double v : values) {
            if (v > 0) {
                n++;
            }
        }
        return n;
    }

    private static void ring(Page page, float cx, float cy, float outerR, float innerR,
                             double fromDeg, double toDeg, int color) {
        int steps = Math.max(2, (int) Math.ceil(Math.abs(toDeg - fromDeg) / 3));
        float[] xs = new float[(steps + 1) * 2];
        float[] ys = new float[(steps + 1) * 2];

        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(fromDeg + (toDeg - fromDeg) * i / steps);
            xs[i] = (float) (cx + outerR * Math.cos(angle));
            ys[i] = (float) (cy + outerR * Math.sin(angle));
        }
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(toDeg - (toDeg - fromDeg) * i / steps);
            xs[steps + 1 + i] = (float) (cx + innerR * Math.cos(angle));
            ys[steps + 1 + i] = (float) (cy + innerR * Math.sin(angle));
        }

        page.fillColor(color).polygon(xs, ys, true, false);
    }

    static float horizontalBars(Page page, float x, float topY, float width, float labelWidth,
                                List<String> labels, double[] values, int[] colors, List<String> valueLabels) {
        float rowHeight = 20;
        float barHeight = 11;
        double max = 0;
        for (double v : values) {
            max = Math.max(max, v);
        }

        float valueRoom = 78;
        float barArea = width - labelWidth - valueRoom;

        for (int i = 0; i < labels.size(); i++) {
            float rowTop = topY + i * rowHeight;
            float baseline = y(rowTop + 12);

            page.fillColor(ReportPalette.TEXT).text(x, baseline, Font.REGULAR, 8.5f,
                    PdfFonts.fit(Font.REGULAR, labels.get(i), 8.5f, labelWidth - 6));

            page.fillColor(ReportPalette.GRID).roundRect(x + labelWidth, y(rowTop + 3 + barHeight), barArea, barHeight, 2, true, false);

            float filled = max <= 0 ? 0 : (float) (values[i] / max * barArea);
            if (values[i] > 0) {
                page.fillColor(colors[i]).roundRect(x + labelWidth, y(rowTop + 3 + barHeight), Math.max(filled, 3), barHeight, 2, true, false);
            }

            page.fillColor(ReportPalette.TEXT).text(x + labelWidth + barArea + 6, baseline, Font.BOLD, 8.5f, valueLabels.get(i));
        }
        return labels.size() * rowHeight;
    }

    static void stackedBar(Page page, float x, float topY, float width, float height, double[] values, int[] colors) {
        double total = 0;
        for (double v : values) {
            total += v;
        }

        page.fillColor(ReportPalette.GRID).roundRect(x, y(topY + height), width, height, 3, true, false);
        if (total <= 0) {
            return;
        }

        float cursor = x;
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) {
                continue;
            }
            float w = (float) (values[i] / total * width);
            page.fillColor(colors[i]).rect(cursor, y(topY + height), w, height, true, false);
            cursor += w;
        }
    }

    record Plot(float x, float top, float width, float height) {
        float bottom() {
            return top + height;
        }
    }

    static double niceMax(double max) {
        if (max <= 0) {
            return 4;
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
        double normalized = max / magnitude;
        double nice = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
        double result = nice * magnitude;
        return Math.max(result, 4);
    }

    static void axes(Page page, Plot plot, double yMax, List<String> xLabels) {
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            float lineTop = plot.bottom() - plot.height() * i / gridLines;
            page.strokeColor(ReportPalette.GRID).lineWidth(0.5f).line(plot.x(), y(lineTop), plot.x() + plot.width(), y(lineTop));

            double value = yMax * i / gridLines;
            String label = value == Math.floor(value) ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
            page.fillColor(ReportPalette.TEXT_MUTED).textRight(plot.x() - 5, y(lineTop) - 2.5f, Font.REGULAR, 7, label);
        }

        int n = xLabels.size();
        if (n == 0) {
            return;
        }
        int maxLabels = Math.max(2, (int) (plot.width() / 48));
        int every = Math.max(1, (int) Math.ceil(n / (double) maxLabels));
        for (int i = 0; i < n; i += every) {
            page.fillColor(ReportPalette.TEXT_MUTED).textCenter(xAt(plot, i, n), y(plot.bottom() + 11), Font.REGULAR, 7, xLabels.get(i));
        }
    }

    static float xAt(Plot plot, int index, int count) {
        return count <= 1 ? plot.x() + plot.width() / 2 : plot.x() + plot.width() * index / (count - 1);
    }

    private static float yAt(Plot plot, double value, double yMax) {
        return y(plot.bottom() - (float) (value / yMax * plot.height()));
    }

    static void lineChart(Page page, Plot plot, List<String> xLabels, List<Series> series, boolean dashedLast) {
        double max = 0;
        for (Series s : series) {
            for (double v : s.values()) {
                max = Math.max(max, v);
            }
        }
        double yMax = niceMax(max);
        axes(page, plot, yMax, xLabels);

        int n = xLabels.size();
        for (int s = 0; s < series.size(); s++) {
            Series current = series.get(s);
            float[] xs = new float[n];
            float[] ys = new float[n];
            for (int i = 0; i < n; i++) {
                xs[i] = xAt(plot, i, n);
                ys[i] = yAt(plot, current.values()[i], yMax);
            }

            page.strokeColor(current.color()).lineWidth(1.6f);
            if (dashedLast && s == series.size() - 1) {
                page.dashed(4, 3);
            }
            if (n == 1) {
                page.fillColor(current.color()).rect(xs[0] - 2, ys[0] - 2, 4, 4, true, false);
            } else {
                page.polyline(xs, ys);
            }
            page.solid();
        }
    }

    static void stackedAreaChart(Page page, Plot plot, List<String> xLabels, List<Series> stack, Series overlay) {
        int n = xLabels.size();
        double[] totals = new double[n];
        for (Series s : stack) {
            for (int i = 0; i < n; i++) {
                totals[i] += s.values()[i];
            }
        }

        double max = 0;
        for (double t : totals) {
            max = Math.max(max, t);
        }
        if (overlay != null) {
            for (double v : overlay.values()) {
                max = Math.max(max, v);
            }
        }
        double yMax = niceMax(max);
        axes(page, plot, yMax, xLabels);

        double[] lower = new double[n];
        for (Series s : stack) {
            double[] upper = new double[n];
            for (int i = 0; i < n; i++) {
                upper[i] = lower[i] + s.values()[i];
            }

            if (n == 1) {
                float x = xAt(plot, 0, 1);
                float bar = Math.min(40, plot.width() / 3);
                float yTop = yAt(plot, upper[0], yMax);
                float yBase = yAt(plot, lower[0], yMax);
                page.fillColor(s.color()).rect(x - bar / 2, yBase, bar, yTop - yBase, true, false);
            } else {
                float[] xs = new float[n * 2];
                float[] ys = new float[n * 2];
                for (int i = 0; i < n; i++) {
                    xs[i] = xAt(plot, i, n);
                    ys[i] = yAt(plot, upper[i], yMax);
                    xs[n * 2 - 1 - i] = xAt(plot, i, n);
                    ys[n * 2 - 1 - i] = yAt(plot, lower[i], yMax);
                }
                page.fillColor(s.color()).polygon(xs, ys, true, false);
            }
            lower = upper;
        }

        if (overlay != null) {
            float[] xs = new float[n];
            float[] ys = new float[n];
            for (int i = 0; i < n; i++) {
                xs[i] = xAt(plot, i, n);
                ys[i] = yAt(plot, overlay.values()[i], yMax);
            }
            page.strokeColor(overlay.color()).lineWidth(1.6f).dashed(4, 3);
            if (n == 1) {
                page.solid().fillColor(overlay.color()).rect(xs[0] - 2, ys[0] - 2, 4, 4, true, false);
            } else {
                page.polyline(xs, ys);
            }
            page.solid();
        }
    }

    static float legend(Page page, float x, float topY, List<String> labels, int[] colors) {
        float cursor = x;
        for (int i = 0; i < labels.size(); i++) {
            page.fillColor(colors[i]).roundRect(cursor, y(topY + 8), 8, 8, 1.5f, true, false);
            page.fillColor(ReportPalette.TEXT).text(cursor + 12, y(topY + 7), Font.REGULAR, 8, labels.get(i));
            cursor += 12 + PdfFonts.width(Font.REGULAR, labels.get(i), 8) + 14;
        }
        return cursor - x;
    }

    static void emptyState(Page page, Plot plot, String message) {
        page.strokeColor(ReportPalette.BORDER).lineWidth(0.8f).dashed(3, 3)
                .roundRect(plot.x(), y(plot.bottom()), plot.width(), plot.height(), 4, false, true).solid();
        page.fillColor(ReportPalette.TEXT_MUTED).textCenter(plot.x() + plot.width() / 2, y(plot.top() + plot.height() / 2) - 3, Font.ITALIC, 9, message);
    }
}
