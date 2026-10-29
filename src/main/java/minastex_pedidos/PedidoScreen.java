package minastex_pedidos;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Checkbox;
import megalodonte.components.DatePicker;
import megalodonte.components.LineHorizontal;
import megalodonte.components.Select;
import megalodonte.components.Text;
import megalodonte.components.inputs.TextAreaInput;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ColumnProps;
import megalodonte.props.InputProps;
import megalodonte.props.DatePickerProps;
import java.util.Locale;
import megalodonte.props.TextProps;

final class PedidoScreen implements ScreenComponent {
    private final PedidoViewModel model;

    PedidoScreen(AppController app, Store.Order existing) { model = new PedidoViewModel(app, existing); }

    @Override public Component render() {
        return Components.page("Pedido Nº " + model.draft.number,
                Components.row(new Text("Cliente"),
                        new Select<Store.Client>().items(model.app.store.data.clients.stream()
                                .filter(client -> client.active || client.id.equals(model.draft.clientId)).toList())
                                .value(model.client),
                        new Text("Data"), new DatePicker(model.date, new DatePickerProps().pattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))),
                        Components.compactField("IPI (%)", model.ipi, 120)),
                Components.button("+ Grupo de material", model::addGroup),
                new megalodonte.components.StateList<>(model.groups, Components::orderGroup, 15),
                new Text(model.totals, new TextProps().fontSize(ThemeManager.theme().typography().subtitle()).bold()),
                new LineHorizontal(), new Text("Informações complementares"),
                Components.row(Components.field("Peso (kg) — manual", model.weight),
                        Components.field("Frete/condição", model.freight)),
                Components.row(Components.field("Forma de pagamento", model.payment),
                        Components.field("Prazo", model.paymentTerm)),
                Components.row(Components.field("Data de retirada", model.pickupDate),
                        Components.field("Observações", model.notes)),
                new Checkbox("Incluir dados bancários no PDF", model.includeBank),
                megalodonte.v2.Show.when(model.includeBank, () -> new Column(new ColumnProps().spacingOf(8)).children(
                        new Select<Store.Account>().items(model.app.store.data.accounts).value(model.account),
                        new TextAreaInput(model.bankText, new InputProps()))),
                Components.row(
                        Components.button("Salvar pedido", model::save),
                        Components.button("Visualizar PDF", model::preview),
                        Components.button("Exportar PDF", model::export),
                        Components.button("Cancelar", model.app::history)));
    }
}
