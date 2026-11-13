package minastex_pedidos;

import pack.utilities.FormatterPack;
import pack.utilities.pdf.DataPdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps the order domain to the data consumed by the versioned PDF template. */
final class OrderPdf {
    private static final String TEMPLATE = "/pdf/order.jsonc";

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
        data.put("company", Map.of(
                "logo", value(company.logo),
                "name", value(company.name),
                "legalLine", legalLine(company),
                "contactLine", join(company.phone, company.email, company.address),
                "website", value(company.website)
        ));
        data.put("clientLine", join("Cliente: " + value(order.clientName), order.clientDocument));
        data.put("groups", order.groups.stream().map(OrderPdf::group).toList());
        data.put("subtotal", "Subtotal: " + Money.money(Store.subtotal(order)));
        data.put("ipi", "IPI (" + order.ipiPercent.toPlainString() + "%): " + Money.money(Store.ipi(order)));
        data.put("total", "TOTAL: " + Money.money(Store.total(order)));
        data.put("peso", "Peso: " + value(order.weight));
        data.put("frete", "Frete: " + value(order.freight));
        data.put("payment", "Pagamento: " + value(order.payment));
        data.put("prazo", "Prazo: " + value(order.paymentTerm));
        data.put("pickup", optionalLine("Retirada: ", order.pickupDate));
        data.put("notes", optionalLine("Observações: ", order.notes));
        data.put("showBank", order.includeBank && !order.bankText.isBlank());
        data.put("bankText", value(order.bankText));
        return data;
    }

    private static Map<String, Object> group(Store.Group group) {
        return Map.of(
                "title", value(group.materialName) + "  ·  " + Money.money(group.price) + "/m²",
                "items", group.items.stream().map(item -> item(item, group)).toList(),
                "total", "Total do grupo: " + Money.area(Store.groupArea(group))
                        + "   ·   " + Money.money(Store.groupValue(group))
        );
    }

    private static Map<String, String> item(Store.Item item, Store.Group group) {
        return Map.of(
                "length", item.length.toPlainString() + " m",
                "width", item.width.toPlainString() + " m",
                "quantity", Integer.toString(item.quantity),
                "area", Money.area(Store.area(item)),
                "value", Money.money(Store.itemValue(item, group))
        );
    }

    private static String legalLine(Store.Company company) {
        String document = FormatterPack.formatCnpj(company.document);
        return value(company.legalName) + (document.isBlank() ? "" : " - " + document);
    }

    private static String optionalLine(String label, String value) {
        return value == null || value.isBlank() ? "" : label + value.trim();
    }

    private static String join(String... values) {
        return java.util.Arrays.stream(values).filter(value -> value != null && !value.isBlank())
                .map(String::trim).collect(java.util.stream.Collectors.joining("   "));
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
