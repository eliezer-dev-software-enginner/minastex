package minastex_pedidos;

import com.fasterxml.jackson.databind.ObjectMapper;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.components.Ref;
import megalodonte.base.state.State;
import megalodonte.components.DesktopDialogs;
import megalodonte.components.SimpleTable;
import megalodonte.props.SimpleTableProps;
import minastex_pedidos.forms.AccountForm;
import minastex_pedidos.forms.ClientForm;
import minastex_pedidos.forms.MaterialForm;

import java.math.BigDecimal;
import java.util.List;

final class CadastroScreen implements ScreenComponent {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final State<String> query = State.of("");
    private final State<List<Object>> rows = State.of(List.of());
    private final Ref<SimpleTable<Object>> tableRef = new Ref<>();
    private final AppController app;
    private final String kind;

    CadastroScreen(AppController app, String kind) {
        this.app = app; this.kind = kind;
        query.subscribe(ignored -> refresh());
    }

    private List<?> values() {
        return kind.equals("Clientes") ? app.store.data.clients
                : kind.equals("Acabamentos") ? app.store.data.materials : app.store.data.accounts;
    }

    private void refresh() {
        List<Object> filtered = values().stream().filter(value -> value.toString().toLowerCase()
                .contains(query.get().toLowerCase())).map(value -> (Object) value).toList();
        if (filtered.equals(rows.get())) rows.set(List.of());
        rows.set(filtered);
    }

    @Override public Component render() {
        SimpleTable<Object> table = new SimpleTable<>(new SimpleTableProps().maxHeight(500));
        var columns = table.header().columns();
        if (kind.equals("Clientes")) columns.column("Nome", x -> ((Store.Client) x).name)
                .column("CPF/CNPJ", x -> ((Store.Client) x).document)
                .column("Telefone", x -> ((Store.Client) x).phone)
                .column("E-mail", x -> ((Store.Client) x).email)
                .column("Situação", x -> ((Store.Client) x).active ? "Ativo" : "Inativo");
        else if (kind.equals("Acabamentos")) columns.column("Acabamento", x -> ((Store.Material) x).name)
                .column("Preço/m²", x -> Money.money(((Store.Material) x).price))
                .column("Situação", x -> ((Store.Material) x).active ? "Ativo" : "Inativo");
        else columns.column("Identificação", x -> ((Store.Account) x).label)
                .column("Banco", x -> ((Store.Account) x).bank)
                .column("Agência", x -> ((Store.Account) x).agency)
                .column("Conta", x -> ((Store.Account) x).number);
        table.paginate(10).fromData(rows);
        return Components.page(kind,
                disgust.io.Pack.searchInput(query, "Pesquisar", true, null),
                Components.row(
                        Components.button("+ Cadastrar", () -> edit(null)),
                        Components.button("Editar", () -> { if (table.getSelectedItem() != null) edit(table.getSelectedItem()); }),
                        Components.button("Ativar/desativar", () -> toggle(table.getSelectedItem())),
                        Components.button("Excluir", () -> delete(table.getSelectedItem())),
                        Components.button("Limpar seleção", () -> tableRef.current().clearSelection())
                ), table.ref(tableRef), table.paginationControls());
    }

    private void edit(Object existing) {
        if (kind.equals("Clientes")) {
            Store.Client client = existing == null ? new Store.Client() : JSON.convertValue(existing, Store.Client.class);
            ClientForm form = new ClientForm(client);
            if (!DesktopDialogs.form(kind, form.render())) return;
            if (form.name.get().isBlank()) { AppController.alert("Informe o nome."); return; }
            client.name = form.name.get().trim(); client.document = form.document.get().trim();
            client.phone = form.phone.get().trim(); client.email = form.email.get().trim();
            client.notes = form.notes.get().trim(); replace(app.store.data.clients, (Store.Client) existing, client);
        } else if (kind.equals("Acabamentos")) {
            Store.Material material = existing == null ? new Store.Material() : JSON.convertValue(existing, Store.Material.class);
            MaterialForm form = new MaterialForm(material);
            if (!DesktopDialogs.form(kind, form.render())) return;
            try {
                BigDecimal price = Store.decimal(form.price.get());
                if (form.name.get().isBlank() || price.signum() < 0) throw new IllegalArgumentException();
                material.name = form.name.get().trim(); material.price = Store.amount(price);
                replace(app.store.data.materials, (Store.Material) existing, material);
            } catch (Exception error) { AppController.alert("Informe nome e preço válido."); return; }
        } else {
            Store.Account account = existing == null ? new Store.Account() : JSON.convertValue(existing, Store.Account.class);
            AccountForm form = new AccountForm(account);
            if (!DesktopDialogs.form(kind, form.render())) return;
            if (form.label.get().isBlank() || form.bank.get().isBlank()) {
                AppController.alert("Informe identificação e banco."); return;
            }
            account.label = form.label.get().trim(); account.bank = form.bank.get().trim();
            account.agency = form.agency.get().trim(); account.number = form.number.get().trim();
            account.beneficiary = form.beneficiary.get().trim(); account.document = form.document.get().trim();
            account.pix = form.pix.get().trim(); replace(app.store.data.accounts, (Store.Account) existing, account);
        }
        app.save(); refresh();
    }

    private void toggle(Object selected) {
        if (selected instanceof Store.Client client) client.active = !client.active;
        else if (selected instanceof Store.Material material) material.active = !material.active;
        else return;
        app.save(); refresh();
    }

    private void delete(Object selected) {
        if (selected == null || !AppController.confirm("Excluir registro?")) return;
        if (selected instanceof Store.Client client) {
            if (app.store.data.orders.stream().anyMatch(order -> order.clientId.equals(client.id))) {
                AppController.alert("Cliente usado em pedido: desative-o."); return;
            }
            app.store.data.clients.remove(client);
        } else if (selected instanceof Store.Material material) {
            if (app.store.data.orders.stream().flatMap(order -> order.groups.stream())
                    .anyMatch(group -> group.materialId.equals(material.id))) {
                AppController.alert("Acabamento usado em pedido: desative-o."); return;
            }
            app.store.data.materials.remove(material);
        } else app.store.data.accounts.remove(selected);
        app.save(); refresh();
    }

    private static <T> void replace(List<T> values, T old, T next) {
        if (old == null) values.add(next); else values.set(values.indexOf(old), next);
    }
}
