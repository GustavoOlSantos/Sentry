package br.uva.tcc.sentry.report.pdf;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import br.uva.tcc.sentry.report.pdf.PdfFonts.Font;

public final class PdfDocument {

    public static final float PAGE_WIDTH = 595.28f;
    public static final float PAGE_HEIGHT = 841.89f;

    private final List<Page> pages = new ArrayList<>();
    private final List<PdfImage> images = new ArrayList<>();
    private final boolean compress;
    private final String title;
    private final String author;
    private final Instant created;

    public PdfDocument(String title, String author, boolean compress, Instant created) {
        this.title = title;
        this.author = author;
        this.compress = compress;
        this.created = created;
    }

    public Page addPage() {
        Page page = new Page();
        pages.add(page);
        return page;
    }

    public List<Page> pages() {
        return pages;
    }

    public PdfImage addImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        byte[] rgb = new byte[width * height * 3];
        byte[] alpha = new byte[width * height];
        boolean hasAlpha = false;

        int i = 0;
        int a = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getRGB(x, y);
                int alphaValue = (argb >>> 24) & 0xFF;
                rgb[i++] = (byte) ((argb >> 16) & 0xFF);
                rgb[i++] = (byte) ((argb >> 8) & 0xFF);
                rgb[i++] = (byte) (argb & 0xFF);
                alpha[a++] = (byte) alphaValue;
                hasAlpha |= alphaValue != 255;
            }
        }

        PdfImage image = new PdfImage("Im" + (images.size() + 1), width, height, rgb, hasAlpha ? alpha : null);
        images.add(image);
        return image;
    }

    public static final class Page {
        private final StringBuilder ops = new StringBuilder();
        private final Set<PdfImage> usedImages = new LinkedHashSet<>();

        public Page fillColor(int rgb) {
            ops.append(color(rgb)).append(" rg\n");
            return this;
        }

        public Page strokeColor(int rgb) {
            ops.append(color(rgb)).append(" RG\n");
            return this;
        }

        public Page lineWidth(float width) {
            ops.append(n(width)).append(" w\n");
            return this;
        }

        public Page dashed(float on, float off) {
            ops.append('[').append(n(on)).append(' ').append(n(off)).append("] 0 d\n");
            return this;
        }

        public Page solid() {
            ops.append("[] 0 d\n");
            return this;
        }

        public Page save() {
            ops.append("q\n");
            return this;
        }

        public Page restore() {
            ops.append("Q\n");
            return this;
        }

        public Page rect(float x, float y, float w, float h, boolean fill, boolean stroke) {
            ops.append(n(x)).append(' ').append(n(y)).append(' ').append(n(w)).append(' ').append(n(h)).append(" re ");
            return paint(fill, stroke);
        }

        public Page roundRect(float x, float y, float w, float h, float radius, boolean fill, boolean stroke) {
            float r = Math.min(radius, Math.min(w, h) / 2);
            float k = 0.5523f * r;

            move(x + r, y);
            lineTo(x + w - r, y);
            curve(x + w - r + k, y, x + w, y + r - k, x + w, y + r);
            lineTo(x + w, y + h - r);
            curve(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
            lineTo(x + r, y + h);
            curve(x + r - k, y + h, x, y + h - r + k, x, y + h - r);
            lineTo(x, y + r);
            curve(x, y + r - k, x + r - k, y, x + r, y);
            ops.append("h ");
            return paint(fill, stroke);
        }

        public Page line(float x1, float y1, float x2, float y2) {
            move(x1, y1);
            lineTo(x2, y2);
            ops.append("S\n");
            return this;
        }

        public Page polyline(float[] xs, float[] ys) {
            if (xs.length == 0) {
                return this;
            }
            move(xs[0], ys[0]);
            for (int i = 1; i < xs.length; i++) {
                lineTo(xs[i], ys[i]);
            }
            ops.append("S\n");
            return this;
        }

        public Page polygon(float[] xs, float[] ys, boolean fill, boolean stroke) {
            if (xs.length == 0) {
                return this;
            }
            move(xs[0], ys[0]);
            for (int i = 1; i < xs.length; i++) {
                lineTo(xs[i], ys[i]);
            }
            ops.append("h ");
            return paint(fill, stroke);
        }

        public Page text(float x, float y, Font font, float size, String text) {
            String clean = PdfFonts.sanitize(text);
            if (clean.isEmpty()) {
                return this;
            }
            ops.append("BT /").append(font.resourceName).append(' ').append(n(size)).append(" Tf ")
                    .append(n(x)).append(' ').append(n(y)).append(" Td (").append(escape(clean)).append(") Tj ET\n");
            return this;
        }

        public Page textRight(float xRight, float y, Font font, float size, String text) {
            return text(xRight - PdfFonts.width(font, text, size), y, font, size, text);
        }

        public Page textCenter(float xCenter, float y, Font font, float size, String text) {
            return text(xCenter - PdfFonts.width(font, text, size) / 2, y, font, size, text);
        }

        public Page image(PdfImage image, float x, float y, float w, float h) {
            usedImages.add(image);
            ops.append("q ").append(n(w)).append(" 0 0 ").append(n(h)).append(' ').append(n(x)).append(' ').append(n(y))
                    .append(" cm /").append(image.name).append(" Do Q\n");
            return this;
        }

        private void move(float x, float y) {
            ops.append(n(x)).append(' ').append(n(y)).append(" m ");
        }

        private void lineTo(float x, float y) {
            ops.append(n(x)).append(' ').append(n(y)).append(" l ");
        }

        private void curve(float x1, float y1, float x2, float y2, float x3, float y3) {
            ops.append(n(x1)).append(' ').append(n(y1)).append(' ').append(n(x2)).append(' ').append(n(y2)).append(' ')
                    .append(n(x3)).append(' ').append(n(y3)).append(" c ");
        }

        private Page paint(boolean fill, boolean stroke) {
            ops.append(fill && stroke ? "B" : fill ? "f" : stroke ? "S" : "n").append('\n');
            return this;
        }

        String content() {
            return ops.toString();
        }
    }

    public static final class PdfImage {
        final String name;
        final int width;
        final int height;
        final byte[] rgb;
        final byte[] alpha;

        PdfImage(String name, int width, int height, byte[] rgb, byte[] alpha) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.rgb = rgb;
            this.alpha = alpha;
        }

        public float aspectRatio() {
            return (float) width / height;
        }
    }

    public byte[] toBytes() {
        try {
            return write();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao montar o PDF", e);
        }
    }

    private byte[] write() throws IOException {
        List<byte[]> objects = new ArrayList<>();
        objects.add(null);

        int next = 4;
        int fontBase = next;
        next += 3;

        int[] imageObjects = new int[images.size()];
        int[] smaskObjects = new int[images.size()];
        for (int i = 0; i < images.size(); i++) {
            imageObjects[i] = next++;
            smaskObjects[i] = images.get(i).alpha != null ? next++ : 0;
        }

        int[] pageObjects = new int[pages.size()];
        int[] contentObjects = new int[pages.size()];
        for (int i = 0; i < pages.size(); i++) {
            pageObjects[i] = next++;
            contentObjects[i] = next++;
        }

        byte[][] bodies = new byte[next][];

        StringBuilder kids = new StringBuilder();
        for (int pageObject : pageObjects) {
            kids.append(pageObject).append(" 0 R ");
        }

        bodies[1] = ascii("<< /Type /Catalog /Pages 2 0 R >>");
        bodies[2] = ascii("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>");
        bodies[3] = ascii("<< /Title (" + escape(PdfFonts.sanitize(title)) + ") /Author (" + escape(PdfFonts.sanitize(author))
                + ") /Creator (TC Sentry) /Producer (TC Sentry) /CreationDate (" + pdfDate(created) + ") >>");

        Font[] fonts = Font.values();
        for (int i = 0; i < fonts.length; i++) {
            bodies[fontBase + i] = ascii("<< /Type /Font /Subtype /Type1 /BaseFont /" + fonts[i].baseFont
                    + " /Encoding /WinAnsiEncoding >>");
        }

        for (int i = 0; i < images.size(); i++) {
            PdfImage image = images.get(i);
            bodies[imageObjects[i]] = stream(
                    "<< /Type /XObject /Subtype /Image /Width " + image.width + " /Height " + image.height
                            + " /ColorSpace /DeviceRGB /BitsPerComponent 8"
                            + (image.alpha != null ? " /SMask " + smaskObjects[i] + " 0 R" : ""),
                    image.rgb, true);
            if (image.alpha != null) {
                bodies[smaskObjects[i]] = stream(
                        "<< /Type /XObject /Subtype /Image /Width " + image.width + " /Height " + image.height
                                + " /ColorSpace /DeviceGray /BitsPerComponent 8",
                        image.alpha, true);
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);

            StringBuilder xobjects = new StringBuilder();
            for (PdfImage used : page.usedImages) {
                xobjects.append('/').append(used.name).append(' ').append(imageObjects[images.indexOf(used)]).append(" 0 R ");
            }

            bodies[pageObjects[i]] = ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(PAGE_WIDTH) + ' ' + n(PAGE_HEIGHT)
                    + "] /Resources << /Font << /F1 " + fontBase + " 0 R /F2 " + (fontBase + 1) + " 0 R /F3 " + (fontBase + 2)
                    + " 0 R >>" + (xobjects.length() > 0 ? " /XObject << " + xobjects + ">>" : "")
                    + " >> /Contents " + contentObjects[i] + " 0 R >>");
            bodies[contentObjects[i]] = stream("<<", page.content().getBytes(StandardCharsets.ISO_8859_1), compress);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ascii("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n"));

        int[] offsets = new int[next];
        for (int id = 1; id < next; id++) {
            offsets[id] = out.size();
            out.write(ascii(id + " 0 obj\n"));
            out.write(bodies[id]);
            out.write(ascii("\nendobj\n"));
        }

        int xref = out.size();
        StringBuilder table = new StringBuilder("xref\n0 " + next + "\n0000000000 65535 f \n");
        for (int id = 1; id < next; id++) {
            table.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[id]));
        }
        out.write(ascii(table.toString()));
        out.write(ascii("trailer\n<< /Size " + next + " /Root 1 0 R /Info 3 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));

        return out.toByteArray();
    }

    private static byte[] stream(String dictStart, byte[] data, boolean compress) throws IOException {
        byte[] payload = data;
        String filter = "";
        if (compress) {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed, new Deflater(Deflater.BEST_COMPRESSION))) {
                deflater.write(data);
            }
            payload = compressed.toByteArray();
            filter = " /Filter /FlateDecode";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(ascii(dictStart + filter + " /Length " + payload.length + " >>\nstream\n"));
        out.write(payload);
        out.write(ascii("\nendstream"));
        return out.toByteArray();
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String pdfDate(Instant instant) {
        return "D:" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC).format(instant) + "Z";
    }

    private static String color(int rgb) {
        return n(((rgb >> 16) & 0xFF) / 255f) + " " + n(((rgb >> 8) & 0xFF) / 255f) + " " + n((rgb & 0xFF) / 255f);
    }

    static String n(float value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        if (text.indexOf('.') >= 0) {
            text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return text.equals("-0") ? "0" : text;
    }

    static String escape(String text) {
        StringBuilder out = new StringBuilder();
        for (byte b : text.getBytes(PdfFonts.WIN_ANSI)) {
            int code = b & 0xFF;
            if (code == '(' || code == ')' || code == '\\') {
                out.append('\\').append((char) code);
            } else if (code >= 32 && code <= 126) {
                out.append((char) code);
            } else {
                out.append(String.format("\\%03o", code));
            }
        }
        return out.toString();
    }
}
