package minastex_pedidos;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    @TempDir Path directory;

    @Test void fillsMissingCompanyContactsWithoutOverwritingExistingValues() throws Exception {
        Path data = directory.resolve("data.json");
        Store store = new Store(data);
        assertEquals(Store.Company.DEFAULT_WEBSITE, store.data.company.website);
        assertEquals(Store.Company.DEFAULT_PHONE, store.data.company.phone);
        store.data.company.phone = "Telefone personalizado";
        store.data.company.email = "";
        store.save();
        Store reopened = new Store(data);
        assertEquals("Telefone personalizado", reopened.data.company.phone);
        assertEquals(Store.Company.DEFAULT_EMAIL, reopened.data.company.email);
    }

    @Test void calculatesBrazilianExampleAndKeepsCommercialSnapshot() throws Exception {
        Store store = new Store(directory.resolve("data.json"));
        Store.Order order = new Store.Order();
        order.number = store.nextNumber();
        order.clientName = "Cliente de teste";
        Store.Group group = new Store.Group();
        group.materialName = "Branco Gelo MTX";
        group.price = new BigDecimal("12.94");
        Store.Item item = new Store.Item();
        item.length = new BigDecimal("2.130");
        item.width = new BigDecimal("0.630");
        item.quantity = 240;
        group.items.add(item);
        order.groups.add(group);
        store.data.orders.add(order);
        store.save();

        assertEquals(new BigDecimal("322.056000"), Store.area(item));
        assertEquals("322,06 m²", Money.area(Store.area(item)));
        assertEquals(new BigDecimal("4167.40"), Store.subtotal(order));
        assertEquals(new BigDecimal("135.44"), Store.ipi(order));
        assertEquals(new BigDecimal("4302.84"), Store.total(order));

        Store reloaded = new Store(directory.resolve("data.json"));
        reloaded.data.materials.get(0).price = new BigDecimal("99.99");
        assertEquals(new BigDecimal("4167.40"), Store.subtotal(reloaded.data.orders.getFirst()));
        reloaded.data.orders.clear();
        reloaded.save();
        assertEquals(2, new Store(directory.resolve("data.json")).nextNumber());
    }

    @Test void exportsPagedPdfWithOptionalBankDetails() throws Exception {
        Store store = new Store(directory.resolve("data.json"));
        store.data.company.legalName = "MINASTEX IND. DE CORTE E PAINEIS LTDA";
        store.data.company.document = "06340287000185";
        Store.Order order = new Store.Order();
        order.number = 1; order.date = "2026-11-10"; order.clientName = "Cliente Minastex"; order.includeBank = true;
        order.bankText = "Banco Teste | PIX abc123";
        Store.Group group = new Store.Group(); group.materialName = "Branco Gelo MTX";
        group.price = new BigDecimal("12.94");
        for (int i = 0; i < 90; i++) group.items.add(new Store.Item());
        order.groups.add(group);
        Path pdf = directory.resolve("pedido.pdf");
        PdfExporter.export(order, store.data.company, pdf);
        Path previewPdf = Path.of("build/reports/pdf-preview/order.pdf");
        Files.createDirectories(previewPdf.getParent());
        Files.copy(pdf, previewPdf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.size(pdf) > 1000);
        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            assertTrue(document.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.startsWith("MINASTEX - Pedido Nº 1 · 2026-11-10"));
            assertEquals(document.getNumberOfPages(), text.split("MINASTEX - Pedido Nº 1 · 2026-11-10", -1).length - 1);
            assertTrue(text.contains("Cliente Minastex"));
            assertTrue(text.contains(Store.Company.DEFAULT_WEBSITE));
            assertTrue(text.contains(Store.Company.DEFAULT_EMAIL));
            assertTrue(text.contains("MINASTEX IND. DE CORTE E PAINEIS LTDA - 06.340.287/0001-85"));
            assertTrue(text.contains("Banco Teste"));
            Path preview = Path.of("build/reports/pdf-preview/order-page-1.png");
            Files.createDirectories(preview.getParent());
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 144), "png", preview.toFile());
        }
        order.includeBank = false;
        PdfExporter.export(order, store.data.company, pdf);
        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            assertFalse(new PDFTextStripper().getText(document).contains("Banco Teste"));
        }
    }
}
