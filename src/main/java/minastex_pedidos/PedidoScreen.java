package minastex_pedidos;

import disgust.icons.Feather;
import disgust.io.ButtonsPack;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Checkbox;
import megalodonte.components.DatePicker;
import megalodonte.components.LineHorizontal;
import megalodonte.components.Select;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.props.ButtonVariant;
import megalodonte.props.ColumnProps;
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
                        Components.compactField("IPI (%)", model.ipi, 120, minastex_pedidos.data.InputType.DECIMAL_FIXED_TWO)),
                Components.button("+ Grupo de material", model::addGroup),
                new megalodonte.components.StateList<>(model.groups, Components::orderGroup, 15),
                new Text(model.totals, new TextProps().fontSize(ThemeManager.theme().typography().subtitle()).bold()),
                new LineHorizontal(), new Text("Informações complementares"),
                Components.row(Components.field("Peso (kg) — manual", model.weight, minastex_pedidos.data.InputType.DECIMAL_FIXED_TWO),
                        Components.field("Frete/condição", model.freight)),
                Components.row(Components.field("Forma de pagamento", model.payment),
                        Components.field("Prazo", model.paymentTerm)),
                Components.row(Components.field("Data de retirada", model.pickupDate),
                        Components.field("Observações", model.notes)),
                new Checkbox("Incluir dados bancários no PDF", model.includeBank),
                megalodonte.v2.Show.when(model.includeBank, () -> new Column(new ColumnProps().spacingOf(8)).children(
                        new Select<Store.Account>().items(model.app.store.data.accounts).value(model.account),
                        disgust.io.Pack.TextAreaColumn("Dados bancários", model.bankText, "Dados bancários"))),
                Components.row(
                        ButtonsPack.ContainedButtonWithIconStart("Salvar pedido", ButtonVariant.SUCCESS, Feather.SAVE, model::save),
                        Components.button("Visualizar PDF", model::preview),
                        ButtonsPack.ContainedButtonWithIconStart("Exportar PDF", ButtonVariant.PRIMARY, Feather.FILE, model::export),
                        ButtonsPack.ContainedButtonWithIconStart("Cancelar", ButtonVariant.DANGER, Feather.X, model.app::history)
                ));
    }
}
