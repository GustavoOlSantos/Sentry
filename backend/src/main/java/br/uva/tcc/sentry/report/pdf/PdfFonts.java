package br.uva.tcc.sentry.report.pdf;

import java.nio.charset.Charset;

/**
 * Métricas das fontes base-14 do PDF (Helvetica) para cálculo exato de quebra de linha,
 * truncamento com reticências e alinhamento à direita/centro, além da conversão para WinAnsi.
 */
public final class PdfFonts {

    public static final Charset WIN_ANSI = Charset.forName("windows-1252");

    public enum Font {
        REGULAR("F1", "Helvetica", false),
        BOLD("F2", "Helvetica-Bold", true),
        ITALIC("F3", "Helvetica-Oblique", false);

        public final String resourceName;
        public final String baseFont;
        final boolean bold;

        Font(String resourceName, String baseFont, boolean bold) {
            this.resourceName = resourceName;
            this.baseFont = baseFont;
            this.bold = bold;
        }
    }

    private PdfFonts() {
    }

    /** Substitui caracteres fora da tabela Windows-1252 por equivalentes seguros. */
    public static String sanitize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text
                .replace('—', '-')
                .replace('–', '-')
                .replace('‘', '\'')
                .replace('’', '\'')
                .replace('“', '"')
                .replace('”', '"')
                .replace("…", "...")
                .replace("≥", ">=")
                .replace("≤", "<=")
                .replace(' ', ' ');

        StringBuilder sb = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                sb.append(' ');
            } else if (canEncodeWinAnsi(ch)) {
                sb.append(ch);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static boolean canEncodeWinAnsi(char ch) {
        if (ch >= 32 && ch <= 126) {
            return true;
        }
        byte[] encoded = String.valueOf(ch).getBytes(WIN_ANSI);
        String decoded = new String(encoded, WIN_ANSI);
        return decoded.length() == 1 && decoded.charAt(0) == ch;
    }

    /** Largura estimada do texto em pontos PDF (1/72 polegada), baseada nas tabelas AFM da Helvetica. */
    public static float width(Font font, String text, float size) {
        String clean = sanitize(text);
        if (clean.isEmpty()) {
            return 0f;
        }
        int totalUnits = 0;
        for (int i = 0; i < clean.length(); i++) {
            totalUnits += charWidthUnits(clean.charAt(i), font.bold);
        }
        return (totalUnits / 1000f) * size;
    }

    /** Corta o texto com "..." para caber dentro de {@code maxWidth}. */
    public static String fit(Font font, String text, float size, float maxWidth) {
        String clean = sanitize(text);
        if (maxWidth <= 0 || clean.isEmpty()) {
            return "";
        }
        if (width(font, clean, size) <= maxWidth) {
            return clean;
        }
        String ellipsis = "...";
        float ellipsisW = width(font, ellipsis, size);
        if (ellipsisW >= maxWidth) {
            return "";
        }
        int end = clean.length();
        while (end > 0 && width(font, clean.substring(0, end), size) + ellipsisW > maxWidth) {
            end--;
        }
        return clean.substring(0, end).stripTrailing() + ellipsis;
    }

    private static int charWidthUnits(char ch, boolean bold) {
        if (ch == ' ') return 278;
        if (ch == 'i' || ch == 'l' || ch == '!' || ch == '.' || ch == ',' || ch == ':' || ch == ';' || ch == '|' || ch == '\'') {
            return bold ? 280 : 240;
        }
        if (ch == 'I' || ch == 'j' || ch == 'f' || ch == 'r' || ch == 't' || ch == '-' || ch == '(' || ch == ')' || ch == '[' || ch == ']') {
            return bold ? 340 : 300;
        }
        if (ch >= '0' && ch <= '9') {
            return 556;
        }
        if (ch == 'm' || ch == 'w' || ch == 'M' || ch == 'W' || ch == '@' || ch == '%') {
            return bold ? 860 : 810;
        }
        if (ch >= 'A' && ch <= 'Z') {
            return bold ? 710 : 667;
        }
        return bold ? 575 : 535;
    }
}
