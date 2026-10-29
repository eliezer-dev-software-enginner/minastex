package minastex_pedidos;

import disgust.io.ButtonsPack;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Scroll;
import megalodonte.components.Text;
import megalodonte.components.inputs.Input;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ColumnProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;
import megalodonte.props.InputProps;

/** Stateless factories for the application's shared visual vocabulary. */
final class Components {
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

    static Component field(String label, State<String> value) {
        return new Column(new ColumnProps().spacingOf(4).fillWidth()).children(
                new Text(label), new Input(value, new InputProps().placeHolder(label).fillWidth())
        );
    }

    static Component compactField(String label, State<String> value, int width) {
        return new Column(new ColumnProps().spacingOf(4)).children(
                new Text(label), new Input(value, new InputProps().placeHolder(label).width(width))
        );
    }

    static Component orderGroup(PedidoViewModel.GroupViewModel group) {
        return new megalodonte.components.layout_components.Container(
                new megalodonte.props.ContainerProps().themeSurface().paddingAll(14).spacingOf(9)).children(
                row(new megalodonte.components.Select<Store.Material>()
                                .items(group.order.app.store.data.materials.stream()
                                        .filter(material -> material.active || material.id.equals(group.data.materialId)).toList())
                                .value(group.material),
                        compactField("Preço/m²", group.price, 130),
                        button("Remover grupo", () -> group.order.removeGroup(group))),
                row(new Text("Comprimento (m)"), new Text("Largura (m)"),
                        new Text("Qtd."), new Text("Metragem"), new Text("Valor")),
                new megalodonte.components.StateList<>(group.items, Components::orderItem, 8),
                button("+ Linha", group::addItem),
                new Text(group.subtotal)
        );
    }

    static Component orderItem(PedidoViewModel.ItemViewModel item) {
        return row(compactField("Comprimento (m)", item.length, 115),
                compactField("Largura (m)", item.width, 115),
                compactField("Qtd.", item.quantity, 75),
                new Text(item.area), new Text(item.value),
                button("Excluir", () -> item.group.removeItem(item)));
    }

    static Component form(String title, Component... fields) {
        return new Column(new ColumnProps().spacingOf(9).paddingAll(18)).children(
                new Text(title, new TextProps().bold()),
                new Column(new ColumnProps().spacingOf(9)).children(fields));
    }
}
