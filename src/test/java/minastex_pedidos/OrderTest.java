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

    @Test void exportsBrandedSinglePageOrder() throws Exception {
        Store store = new Store(directory.resolve("branded-data.json"));
        Store.Order order = new Store.Order();
        order.number = 37901; order.date = "2026-10-28";
        order.clientName = "PLAYART Digital Material Publicitário Eirelli";
        order.clientDocument = "30617731000141"; order.clientPhone = "32988679585";
        order.clientEmail = "compras@playart.com.br";
        order.clientAddress = "Praça Getúlio Vargas, 80 - Centro - Ubá/MG";
        order.payment = "À vista (PIX)";
        order.paymentTerm = "Retirada"; order.freight = "0";
        order.notes = "Produção conforme medidas conferidas pelo cliente.";
        Store.Group group = new Store.Group();
        group.materialName = "Pré Cortado MDF CRU"; group.price = new BigDecimal("14.42");
        Store.Item item = new Store.Item();
        item.length = new BigDecimal("0.900"); item.width = new BigDecimal("0.600"); item.quantity = 80;
        group.items.add(item); order.groups.add(group);
        Path pdf = Path.of("build/reports/pdf-preview/order-branded.pdf");
        Files.createDirectories(pdf.getParent());

        OrderPdf.write(order, store.data.company, pdf);

        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("PLAYART Digital Material Publicitário Eirelli"));
            assertTrue(text.contains("Praça Getúlio Vargas, 80 - Centro - Ubá/MG"));
            assertTrue(text.contains("Pré Cortado MDF CRU"));
            assertTrue(text.contains("0.900 × 0.600 m"));
            assertTrue(text.contains("80 peças"));
            assertTrue(text.contains("INFORMAÇÕES COMERCIAIS"));
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 144), "png",
                    Path.of("build/reports/pdf-preview/order-branded-page-1.png").toFile());
        }
    }

    @Test void omitsEveryEmptyOptionalBlock() throws Exception {
        Store store = new Store(directory.resolve("minimal-data.json"));
        Store.Order order = new Store.Order();
        order.number = 2; order.date = "2026-11-23"; order.clientName = "Cliente sem complementos";
        Store.Group group = new Store.Group();
        group.materialName = "MDF Branco"; group.price = new BigDecimal("10.00");
        Store.Item item = new Store.Item();
        item.length = BigDecimal.ONE; item.width = BigDecimal.ONE; item.quantity = 1;
        group.items.add(item); order.groups.add(group);
        Path pdf = Path.of("build/reports/pdf-preview/order-without-optionals.pdf");
        Files.createDirectories(pdf.getParent());

        OrderPdf.write(order, store.data.company, pdf);

        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertAll(
                    () -> assertFalse(text.contains("CPF / CNPJ")),
                    () -> assertFalse(text.contains("TELEFONE")),
                    () -> assertFalse(text.contains("E-MAIL")),
                    () -> assertFalse(text.contains("ENDEREÇO")),
                    () -> assertFalse(text.contains("OBSERVAÇÕES")),
                    () -> assertFalse(text.contains("INFORMAÇÕES COMERCIAIS")),
                    () -> assertFalse(text.contains("FORMA DE PAGAMENTO")),
                    () -> assertFalse(text.contains("FRETE")),
                    () -> assertFalse(text.contains("PRAZO")),
                    () -> assertFalse(text.contains("DADOS BANCÁRIOS")));
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 144), "png",
                    Path.of("build/reports/pdf-preview/order-without-optionals-page-1.png").toFile());
        }
    }

    @Test void exportsExactlyTwentyItemsAcrossPages() throws Exception {
        Store store = new Store(directory.resolve("twenty-items-data.json"));
        Store.Order order = new Store.Order();
        order.number = 20; order.date = "2026-11-23"; order.clientName = "Cliente vinte itens";
        for (int index = 1; index <= 20; index++) {
            Store.Group group = new Store.Group();
            group.materialName = "Painel " + String.format("%02d", index);
            group.price = new BigDecimal("12.50");
            Store.Item item = new Store.Item();
            item.length = new BigDecimal("1." + String.format("%02d", index));
            item.width = new BigDecimal("0.50"); item.quantity = index;
            group.items.add(item); order.groups.add(group);
        }
        Path pdf = Path.of("build/reports/pdf-preview/order-20-items.pdf");
        Files.createDirectories(pdf.getParent());

        OrderPdf.write(order, store.data.company, pdf);

        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            assertTrue(document.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(document);
            assertEquals(20, text.split("Painel ", -1).length - 1);
            assertTrue(text.contains("Painel 01"));
            assertTrue(text.contains("Painel 20"));
            assertEquals(document.getNumberOfPages(), text.split("Nº 20", -1).length - 1);
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                ImageIO.write(renderer.renderImageWithDPI(page, 144), "png",
                        Path.of("build/reports/pdf-preview/order-20-items-page-" + (page + 1) + ".png").toFile());
            }
        }
    }

    @Test void exportsCompleteOrderWithEveryAvailableField() throws Exception {
        Store store = new Store(directory.resolve("complete-order-data.json"));
        store.data.company.name = "Minastex";
        store.data.company.legalName = "MINASTEX IND. DE CORTE E PAINÉIS LTDA";
        store.data.company.document = "06340287000185";
        store.data.company.phone = "3235324583";
        store.data.company.email = "minastex@minastex.com.br";
        store.data.company.website = "www.minastex.com.br";
        store.data.company.address = "Ubá - Minas Gerais";
        Store.Order order = new Store.Order();
        order.number = 48021; order.date = "2026-11-23";
        order.clientName = "Cliente Completo Indústria e Comércio Ltda";
        order.clientDocument = "12345678000190";
        order.clientPhone = "32999998888";
        order.clientEmail = "compras@cliente.com.br";
        order.clientAddress = "Avenida Governador Valadares, 1500 - Centro - Ubá/MG - CEP 36500-000";
        order.weight = "1.250 kg";
        order.freight = "18550";
        order.payment = "50% na aprovação e 50% na retirada";
        order.paymentTerm = "15 dias úteis";
        order.pickupDate = "10/12/2026";
        order.notes = "Produzir conforme medidas conferidas. Material identificado por lote e protegido para transporte.";
        order.includeBank = true;
        order.bankText = "Banco Cooperativo | Agência 0001 | Conta 12345-6 | MINASTEX IND. DE CORTE E PAINÉIS LTDA";
        order.pixAccount = "06.340.287/0001-85";
        order.ipiPercent = new BigDecimal("3.25");

        Store.Group cru = new Store.Group();
        cru.materialName = "Pré Cortado MDF CRU 25 mm"; cru.price = new BigDecimal("14.42");
        Store.Item painelMaior = new Store.Item();
        painelMaior.length = new BigDecimal("0.900"); painelMaior.width = new BigDecimal("0.600");
        painelMaior.quantity = 80; cru.items.add(painelMaior);
        Store.Item painelMenor = new Store.Item();
        painelMenor.length = new BigDecimal("0.450"); painelMenor.width = new BigDecimal("0.300");
        painelMenor.quantity = 40; cru.items.add(painelMenor);
        order.groups.add(cru);

        Store.Group branco = new Store.Group();
        branco.materialName = "MDF Branco TX 18 mm"; branco.price = new BigDecimal("18.90");
        Store.Item acabamento = new Store.Item();
        acabamento.length = new BigDecimal("1.200"); acabamento.width = new BigDecimal("0.550");
        acabamento.quantity = 24; branco.items.add(acabamento); order.groups.add(branco);

        Path pdf = Path.of("build/reports/pdf-preview/order-complete.pdf");
        Files.createDirectories(pdf.getParent());

        OrderPdf.write(order, store.data.company, pdf);

        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            String compact = text.replaceAll("\\s+", "");
            assertAll(
                    () -> assertTrue(text.contains("Nº 48021")),
                    () -> assertTrue(text.contains("MINASTEX IND. DE CORTE E PAINÉIS LTDA - 06.340.287/0001-85")),
                    () -> assertTrue(text.contains("Ubá - Minas Gerais")),
                    () -> assertTrue(text.contains("(32) 3532-4583")),
                    () -> assertTrue(text.contains("minastex@minastex.com.br")),
                    () -> assertTrue(text.contains("Cliente Completo Indústria e Comércio Ltda")),
                    () -> assertTrue(text.contains("12.345.678/0001-90")),
                    () -> assertTrue(text.contains("(32) 99999-8888")),
                    () -> assertTrue(compact.contains("compras@cliente.com.br")),
                    () -> assertTrue(text.contains("Avenida Governador Valadares")),
                    () -> assertTrue(text.contains("Pré Cortado MDF CRU 25 mm")),
                    () -> assertTrue(text.contains("0.900 × 0.600 m")),
                    () -> assertTrue(text.contains("MDF Branco TX 18 mm")),
                    () -> assertTrue(compact.contains("50%naaprovaçãoe50%naretirada")),
                    () -> assertTrue(text.contains("15 dias úteis")),
                    () -> assertTrue(text.contains("Retirada: 10/12/2026")),
                    () -> assertTrue(text.contains("Peso: 1.250 kg")),
                    () -> assertTrue(text.contains("DADOS BANCÁRIOS")),
                    () -> assertTrue(text.contains("PIX: 06.340.287/0001-85")));
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 144), "png",
                    Path.of("build/reports/pdf-preview/order-complete-page-1.png").toFile());
        }
    }

    @Test void exportsPagedPdfWithOptionalBankDetails() throws Exception {
        Store store = new Store(directory.resolve("data.json"));
        store.data.company.legalName = "MINASTEX IND. DE CORTE E PAINEIS LTDA";
        store.data.company.document = "06340287000185";
        Store.Order order = new Store.Order();
        order.number = 1; order.date = "2026-11-10"; order.clientName = "Cliente Minastex";
        order.clientDocument = "30617731000141"; order.clientPhone = "32988679585";
        order.clientEmail = "compras@cliente.com.br";
        order.clientAddress = "Rua de teste, 123 - Centro"; order.includeBank = true;
        order.bankText = "Banco Teste | PIX abc123";
        Store.Group group = new Store.Group(); group.materialName = "Branco Gelo MTX";
        group.price = new BigDecimal("12.94");
        for (int i = 0; i < 90; i++) group.items.add(new Store.Item());
        order.groups.add(group);
        Path pdf = directory.resolve("pedido.pdf");
        OrderPdf.write(order, store.data.company, pdf);
        Path previewPdf = Path.of("build/reports/pdf-preview/order.pdf");
        Files.createDirectories(previewPdf.getParent());
        Files.copy(pdf, previewPdf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.size(pdf) > 1000);
        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            assertTrue(document.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("PEDIDO"));
            assertEquals(document.getNumberOfPages(), text.split("Nº 1", -1).length - 1);
            assertEquals(document.getNumberOfPages(), text.split("10 de novembro de 2026", -1).length - 1);
            assertTrue(text.contains("Cliente Minastex"));
            assertTrue(text.contains("30.617.731/0001-41"));
            assertTrue(text.contains("(32) 98867-9585"));
            assertTrue(text.contains("compras@cliente.com.br"));
            assertTrue(text.contains("Rua de teste, 123 - Centro"));
            assertTrue(text.contains("ITENS DO PEDIDO"));
            assertFalse(text.contains("INFORMAÇÕES COMERCIAIS"));
            assertTrue(text.contains("Banco Teste"));
            Path preview = Path.of("build/reports/pdf-preview/order-page-1.png");
            Files.createDirectories(preview.getParent());
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 144), "png", preview.toFile());
        }
        order.includeBank = false;
        OrderPdf.write(order, store.data.company, pdf);
        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            assertFalse(new PDFTextStripper().getText(document).contains("Banco Teste"));
        }
    }
}
