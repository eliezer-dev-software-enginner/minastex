package minastex_pedidos;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Image;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.ImageProps;
import megalodonte.props.TextProps;

import java.util.Comparator;
import java.util.List;

final class InicioScreen implements ScreenComponent {
    private final AppController app;
    InicioScreen(AppController app) { this.app = app; }

    @Override public Component render() {
        return Components.page("Painel de pedidos",
                new Image(InicioScreen.class.getResource("/assets/minastex-logo.png").toExternalForm(),
                        new ImageProps().width(230).preserveRatio(true)),
                new Text("Pedidos comerciais com cálculos automáticos e PDF."),
                Components.row(Components.button("+ Novo pedido", () -> app.order(null)),
                        Components.button("Histórico", app::history)),
                app.ads.highlights(),
                new Text(app.store.data.orders.size() + " pedidos cadastrados"),
                new Text("Últimos pedidos criados", new TextProps().bold()),
                new Column(new ColumnProps().spacingOf(8)).items(latestOrders(app.store.data.orders),
                        order -> Components.button(
                                "Nº " + order.number + " · " + order.clientName + " · " + Money.money(Store.total(order)),
                                () -> app.order(order)))
        );
    }

    static List<Store.Order> latestOrders(List<Store.Order> orders) {
        return orders.stream()
                .sorted(Comparator.comparingInt((Store.Order order) -> order.number).reversed())
                .limit(3)
                .toList();
    }
}
