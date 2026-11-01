package minastex_pedidos;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.components.Ref;
import megalodonte.base.state.State;
import megalodonte.components.SimpleTable;
import megalodonte.components.inputs.Input;
import megalodonte.props.SimpleTableProps;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

final class HistoricoScreen implements ScreenComponent {
    private final State<String> query = State.of("");
    private final State<List<Store.Order>> rows = State.of(List.of());
    private final Ref<SimpleTable<Store.Order>> tableRef = new Ref<>();
    private final AppController app;

    HistoricoScreen(AppController app) {
        this.app = app;
        query.subscribe(ignored -> refresh());
    }

    private void refresh() {
        String filter = query.get().toLowerCase();
        rows.set(app.store.data.orders.stream()
                .filter(order -> (order.number + " " + order.clientName).toLowerCase().contains(filter))
                .sorted(Comparator.comparingInt((Store.Order order) -> order.number).reversed()).toList());
    }

    @Override public Component render() {
        SimpleTable<Store.Order> table = new SimpleTable<>(new SimpleTableProps().maxHeight(500));
        table.header().columns().column("Nº", order -> Integer.toString(order.number))
                .column("Cliente", order -> order.clientName).column("Data", order -> order.date)
                .column("Total", order -> Money.money(Store.total(order)));
        table.paginate(10).fromData(rows).onItemDoubleClick(app::order);
        return Components.page("Histórico de pedidos",
                disgust.io.Pack.searchInput(query, "Pesquisar número ou cliente", true, null),
                Components.row(
                        Components.button("Abrir / editar", () -> selected(table, app::order)),
                        Components.button("Duplicar", () -> selected(table, old -> {
                            Store.Order copy = Store.copy(old);
                            copy.id = Store.id(); copy.number = app.store.nextNumber();
                            copy.date = LocalDate.now().toString(); app.order(copy);
                        })),
                        Components.button("Visualizar PDF", () -> selected(table, app::preview)),
                        Components.button("Exportar PDF", () -> selected(table, app::export)),
                        Components.button("Excluir", () -> selected(table, order -> {
                            if (AppController.confirm("Excluir pedido Nº " + order.number + "?")) {
                                app.store.data.orders.remove(order); app.save(); refresh();
                            }
                        })),
                        Components.button("Limpar seleção", () -> tableRef.current().clearSelection())
                ), table.ref(tableRef), table.paginationControls());
    }

    private static void selected(SimpleTable<Store.Order> table, java.util.function.Consumer<Store.Order> action) {
        Store.Order order = table.getSelectedItem();
        if (order != null) action.accept(order);
    }
}
