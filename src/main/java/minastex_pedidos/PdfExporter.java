package minastex_pedidos;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Paginated A4 commercial order with embedded Unicode font. */
public final class PdfExporter {
    private PdfExporter() { }

    public static void export(Store.Order order, Store.Company company, Path output) throws IOException {
        try (PDDocument doc = new PDDocument(); InputStream fontFile = PdfExporter.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (fontFile == null) throw new IOException("Fonte do PDF não encontrada");
            PDType0Font font = PDType0Font.load(doc, fontFile);
            Writer writer = new Writer(doc, font);
            writer.newPage();
            writer.heading(company, order);
            for (Store.Group group : order.groups) {
                writer.space(14);
                writer.ensure(55);
                writer.line(group.materialName + "  ·  " + Money.money(group.price) + "/m²", 12, true);
                writer.tableRule();
                writer.cells(new String[] {"Comprimento", "Largura", "Qtd.", "Metragem", "Valor"}, 9);
                writer.tableRule();
                for (Store.Item item : group.items) {
                    writer.ensure(22);
                    writer.cells(new String[] {item.length.toPlainString() + " m", item.width.toPlainString() + " m",
                            Integer.toString(item.quantity), Money.area(Store.area(item)), Money.money(Store.itemValue(item, group))}, 9);
                }
                writer.tableRule();
                writer.line("Total do grupo: " + Money.area(Store.groupArea(group)) + "   ·   " + Money.money(Store.groupValue(group)), 10, true);
            }
            writer.space(20);
            writer.ensure(120);
            writer.line("Subtotal: " + Money.money(Store.subtotal(order)), 11, true);
            writer.line("IPI (" + order.ipiPercent.toPlainString() + "%): " + Money.money(Store.ipi(order)), 11, false);
            writer.line("TOTAL: " + Money.money(Store.total(order)), 16, true);
            writer.space(15);
            writer.wrap("Peso: " + order.weight + "   Frete: " + order.freight, 10);
            writer.wrap("Pagamento: " + order.payment + "   Prazo: " + order.paymentTerm, 10);
            if (!order.pickupDate.isBlank()) writer.wrap("Retirada: " + order.pickupDate, 10);
            if (!order.notes.isBlank()) writer.wrap("Observações: " + order.notes, 10);
            if (order.includeBank && !order.bankText.isBlank()) {
                writer.space(12);
                writer.line("DADOS BANCÁRIOS", 10, true);
                writer.wrap(order.bankText, 10);
            }
            writer.close();
            Files.createDirectories(output.toAbsolutePath().getParent());
            doc.save(output.toFile());
        }
    }

    private static final class Writer {
        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y;
        private int pageNumber;
        private static final float LEFT = 42, RIGHT = 553;
        Writer(PDDocument document, PDType0Font font) { this.document = document; this.font = font; }
        void newPage() throws IOException {
            close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            pageNumber++;
            y = 800;
            line("MINASTEX  ·  PEDIDO COMERCIAL", 9, true);
            y -= 10;
        }
        void heading(Store.Company company, Store.Order order) throws IOException {
            try {
                byte[] logoBytes;
                if (company.logo != null && !company.logo.isBlank() && Files.isRegularFile(Path.of(company.logo))) {
                    logoBytes = Files.readAllBytes(Path.of(company.logo));
                } else {
                    try (InputStream bundled = PdfExporter.class.getResourceAsStream("/assets/minastex-logo.png")) {
                        logoBytes = bundled == null ? null : bundled.readAllBytes();
                    }
                }
                if (logoBytes != null) {
                    PDImageXObject image = PDImageXObject.createFromByteArray(document, logoBytes, "minastex-logo");
                    float scale = Math.min(120f / image.getWidth(), 60f / image.getHeight());
                    float width = image.getWidth() * scale, height = image.getHeight() * scale;
                    stream.drawImage(image, LEFT, y - height, width, height);
                    y -= height + 8;
                }
            } catch (IOException | IllegalArgumentException ignored) { /* An invalid custom logo does not invalidate the PDF. */ }
            line(company.name, 18, true);
            wrap(company.legalName + "   " + company.document, 9);
            wrap(company.phone + "   " + company.email + "   " + company.address, 9);
            wrap(company.website, 9);
            space(12);
            line("Pedido Nº " + order.number + "  ·  " + order.date, 14, true);
            wrap("Cliente: " + order.clientName + "   " + order.clientDocument, 11);
            tableRule();
        }
        void ensure(float height) throws IOException { if (y - height < 65) newPage(); }
        void space(float height) throws IOException { ensure(height); y -= height; }
        void line(String text, int size, boolean bold) throws IOException {
            ensure(size + 12);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(LEFT, y);
            stream.showText(fit(text == null ? "" : text, size));
            stream.endText();
            y -= size + (bold ? 8 : 7);
        }
        void cells(String[] values, int size) throws IOException {
            ensure(size + 12);
            float[] x = {LEFT, 145, 245, 300, 420};
            for (int i = 0; i < values.length; i++) {
                stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(x[i], y);
                stream.showText(values[i]); stream.endText();
            }
            y -= size + 9;
        }
        void wrap(String text, int size) throws IOException {
            String remaining = text == null ? "" : text.replaceAll("\\s+", " ").trim();
            while (!remaining.isEmpty()) {
                int cut = remaining.length();
                while (cut > 1 && width(remaining.substring(0, cut), size) > RIGHT - LEFT) {
                    int space = remaining.lastIndexOf(' ', cut - 1);
                    cut = space > 0 ? space : cut - 1;
                }
                line(remaining.substring(0, cut).trim(), size, false);
                remaining = remaining.substring(cut).trim();
            }
        }
        void tableRule() throws IOException {
            ensure(12);
            stream.moveTo(LEFT, y);
            stream.lineTo(RIGHT, y);
            stream.setLineWidth(.5f);
            stream.stroke();
            y -= 11;
        }
        private String fit(String text, int size) throws IOException {
            if (width(text, size) <= RIGHT - LEFT) return text;
            while (!text.isEmpty() && width(text + "…", size) > RIGHT - LEFT) text = text.substring(0, text.length() - 1);
            return text + "…";
        }
        private float width(String text, int size) throws IOException { return font.getStringWidth(text) * size / 1000f; }
        void close() throws IOException { if (stream != null) { stream.close(); stream = null; } }
    }
}
