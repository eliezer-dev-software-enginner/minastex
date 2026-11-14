package minastex_pedidos;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Image;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.ImageProps;

import java.util.Comparator;

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
                new Column(new ColumnProps().spacingOf(8)).items(app.store.data.orders.stream()
                        .sorted(Comparator.comparingInt((Store.Order o) -> o.number).reversed())
                        .limit(5).toList(), order -> Components.button(
                                "Nº " + order.number + " · " + order.clientName + " · " + Money.money(Store.total(order)),
                                () -> app.order(order)))
        );
    }
}
