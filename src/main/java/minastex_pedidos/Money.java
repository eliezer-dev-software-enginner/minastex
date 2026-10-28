package minastex_pedidos;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private static final Locale BR = Locale.of("pt", "BR");
    private Money() { }
    public static String money(BigDecimal value) { return NumberFormat.getCurrencyInstance(BR).format(Store.amount(value)); }
    public static String area(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(Store.amount(value)) + " m²";
    }
}
