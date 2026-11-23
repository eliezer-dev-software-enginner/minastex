package minastex_pedidos;

import pack.utilities.CurrencyPack;
import pack.utilities.FormatterPack;
import pack.utilities.pdf.DataPdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Maps the order domain to the data consumed by the versioned PDF template. */
final class OrderPdf {
    private static final String TEMPLATE = "/pdf/order.jsonc";
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter
            .ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));

    private OrderPdf() {
    }

    static void write(Store.Order order, Store.Company company, Path destination) throws IOException {
        try (InputStream template = OrderPdf.class.getResourceAsStream(TEMPLATE)) {
            if (template == null) throw new IOException("Template do pedido não encontrado: " + TEMPLATE);
            DataPdf.write(destination, template, data(order, company));
        }
    }

    private static Map<String, Object> data(Store.Order order, Store.Company company) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", "MINASTEX - Pedido Nº " + order.number + " · " + order.date);
        data.put("order", Map.of(
                "label", "PEDIDO",
                "number", "Nº " + order.number,
                "date", longDate(order.date)
        ));
        data.put("company", Map.of(
                "logo", value(company.logo),
                "name", value(company.name),
                "legalLine", legalLine(company),
                "phone", companyPhone(company.phone),
                "email", value(company.email),
                "website", value(company.website),
                "address", value(company.address),
                "hasLegal", !legalLine(company).isBlank(),
                "hasContact", !value(company.phone).isBlank() || !value(company.email).isBlank(),
                "hasAddress", !value(company.address).isBlank()
        ));
        data.put("client", Map.of(
                "name", value(order.clientName),
                "document", FormatterPack.formatCpfCnpj(order.clientDocument),
                "phone", FormatterPack.formatPhone(order.clientPhone),
                "email", value(order.clientEmail),
                "address", value(order.clientAddress)
        ));
        data.put("hasClientContacts", !value(order.clientDocument).isBlank()
                || !value(order.clientPhone).isBlank() || !value(order.clientEmail).isBlank());
        data.put("items", items(order));
        data.put("subtotal", Money.money(Store.subtotal(order)));
        data.put("ipiLabel", "IPI (" + order.ipiPercent.toPlainString() + "%)");
        data.put("ipi", Money.money(Store.ipi(order)));
        data.put("total", Money.money(Store.total(order)));
        data.put("peso", value(order.weight));
        data.put("payment", value(order.payment));
        data.put("frete", value(order.freight).isBlank() ? ""
                : CurrencyPack.toBRLCurrency(CurrencyPack.deCentavosParaReal(order.freight)));
        data.put("prazo", value(order.paymentTerm));
        data.put("hasCommercialInfo", !value(order.payment).isBlank()
                || !value(order.freight).isBlank() || !value(order.paymentTerm).isBlank());
        data.put("pickup", value(order.pickupDate));
        data.put("notes", value(order.notes));
        data.put("showBank", order.includeBank && !order.bankText.isBlank());
        data.put("bankText", value(order.bankText));
        data.put("pix", value(order.pixAccount));
        return data;
    }

    private static List<Map<String, String>> items(Store.Order order) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Store.Group group : order.groups) {
            for (Store.Item item : group.items) rows.add(item(item, group));
        }
        return rows;
    }

    private static Map<String, String> item(Store.Item item, Store.Group group) {
        return Map.of(
                "description", value(group.materialName) + " · " + item.length.toPlainString()
                        + " × " + item.width.toPlainString() + " m",
                "quantity", item.quantity + (item.quantity == 1 ? " peça" : " peças"),
                "area", Money.area(Store.area(item)),
                "squareMeterPrice", Money.money(group.price),
                "value", Money.money(Store.itemValue(item, group))
        );
    }

    private static String longDate(String value) {
        try { return LocalDate.parse(value).format(LONG_DATE); }
        catch (DateTimeParseException ignored) { return value(value); }
    }

    private static String legalLine(Store.Company company) {
        String document = FormatterPack.formatCnpj(company.document);
        return value(company.legalName) + (document.isBlank() ? "" : " - " + document);
    }

    private static String companyPhone(String phone) {
        String clean = value(phone);
        return clean.replaceAll("\\D", "").length() <= 11 ? FormatterPack.formatPhone(clean) : clean;
    }

    private static String join(String... values) {
        return java.util.Arrays.stream(values).filter(value -> value != null && !value.isBlank())
                .map(String::trim).collect(java.util.stream.Collectors.joining("   "));
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
