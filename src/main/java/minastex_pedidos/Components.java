package minastex_pedidos;

import disgust.io.ButtonsPack;
import disgust.io.Pack;
import megalodonte.components.Select;
import minastex_pedidos.data.Data;
import minastex_pedidos.data.InputType;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Scroll;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ColumnProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;

import java.util.List;

/** Stateless factories for the application's shared visual vocabulary. */
public final class Components {
    private Components() {}

    static Component button(String title, Runnable action) {
        return ButtonsPack.ContainedButton(title, new ButtonsPack.Props().onClick(action::run));
    }

    static Component nav(String title, disgust.icons.Icon icon, Runnable action) {
        return ButtonsPack.TextButton(title, new ButtonsPack.Props().iconStart(icon).onClick(action::run));
    }

    static Row row(Component... children) {
        return new Row(new RowProps().spacingOf(9).centerVertically()).children(children);
    }

    static Component page(String title, Component... children) {
        return new Scroll(new Column(new ColumnProps().spacingOf(14).fillWidth().paddingAll(24)).children(
                new Text(title, new TextProps().fontSize(ThemeManager.theme().typography().title()).bold()),
                new Column(new ColumnProps().spacingOf(14).fillWidth()).children(children)
        )).fillAvailableWidth().fillAvailableHeight();
    }

    public static Component field(String label, State<String> value) {
        return field(label, value, InputType.TEXT);
    }

    public static Component field(String label, State<String> value, InputType type) {
        if (type == null) throw new IllegalArgumentException("Tipo de entrada obrigatório");
        return switch (type) {
            case CPF -> disgust.io.br.Pack.InputColumnCpf(label, value);
            case CNPJ -> disgust.io.br.Pack.InputColumnCnpjAlfanumerico(label, value);
            case CPF_CNPJ -> disgust.io.br.Pack.InputColumnCpfCnpj(label, value);
            case PHONE -> disgust.io.br.Pack.InputColumnPhone(label, value);
            case CEP -> disgust.io.br.Pack.InputColumnCep(label, value);
            case CURRENCY -> disgust.io.br.Pack.InputColumnCurrency(label, value);
            case INTEGER -> Pack.InputColumnNumeric(label, value, label);
            case DECIMAL -> Pack.InputColumnDecimal(label, value, label);
            case DECIMAL_FIXED_TWO -> Pack.InputColumnDecimalFixedTwo(label, value, label);
            case DECIMAL_FIXED_THREE -> Pack.InputColumnDecimalFixedThree(label, value, label);
            case TEXT -> textField(label, value);
        };
    }

    private static Component textField(String label, State<String> value) {
        return Pack.InputColumn(label, value, label);
    }

    static Component compactField(String label, State<String> value, int width) {
        return compactField(label, value, width, InputType.TEXT);
    }

    static Component compactField(String label, State<String> value, int width, InputType type) {
        if (type != InputType.TEXT) return field(label, value, type);
        return Pack.InputColumn(label, value, label, width);
    }

    static Component orderGroup(PedidoViewModel.GroupViewModel group) {
        return new megalodonte.components.layout_components.Container(
                new megalodonte.props.ContainerProps().themeSurface().paddingAll(14).spacingOf(9)).children(
                row(new megalodonte.components.Select<Store.Material>()
                                .items(group.order.app.store.data.materials.stream()
                                        .filter(material -> material.active || material.id.equals(group.data.materialId)).toList())
                                .value(group.material),
                        compactField("Preço/m²", group.price, 130, InputType.DECIMAL_FIXED_TWO),
                        button("Remover grupo", () -> group.order.removeGroup(group))),
                row(new Text("Comprimento (m)"), new Text("Largura (m)"),
                        new Text("Qtd."), new Text("Metragem"), new Text("Valor")),
                new megalodonte.components.StateList<>(group.items, Components::orderItem, 8),
                button("+ Linha", group::addItem),
                new Text(group.subtotal)
        );
    }

    static Component orderItem(PedidoViewModel.ItemViewModel item) {
        return row(compactField("Comprimento (m)", item.length, 115, InputType.DECIMAL_FIXED_THREE),
                compactField("Largura (m)", item.width, 115, InputType.DECIMAL_FIXED_THREE),
                compactField("Qtd.", item.quantity, 75, InputType.INTEGER),
                new Text(item.area), new Text(item.value),
                button("Excluir", () -> item.group.removeItem(item)));
    }

    public static Component form(String title, Component... fields) {
        return new Column(new ColumnProps().spacingOf(9).paddingAll(18)).children(
                new Text(title, new TextProps().bold()),
                new Column(new ColumnProps().spacingOf(9)).children(fields));
    }

    public static Component select(String label, List<String> dataList, State<String> valueState) {
        return new Column().children(
                new Text(label, new TextProps().fontSize(ThemeManager.theme().typography().small())),
                new Select<String>().items(dataList).value(valueState)
        );
    }
}
