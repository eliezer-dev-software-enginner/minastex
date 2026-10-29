package minastex_pedidos;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.components.FilePicker;

import java.math.BigDecimal;

final class EmpresaScreen implements ScreenComponent {
    private final State<String> name;
    private final State<String> legal;
    private final State<String> document;
    private final State<String> phone;
    private final State<String> email;
    private final State<String> website;
    private final State<String> address;
    private final State<String> logo;
    private final State<String> ipi;
    private final AppController app;

    EmpresaScreen(AppController app) {
        this.app = app;
        Store.Company c = app.store.data.company;
        name = State.of(c.name); legal = State.of(c.legalName); document = State.of(c.document);
        phone = State.of(c.phone); email = State.of(c.email); website = State.of(c.website);
        address = State.of(c.address); logo = State.of(c.logo); ipi = State.of(c.ipiPercent.toPlainString());
    }

    @Override public Component render() {
        return Components.page("Configurações da empresa",
                Components.row(Components.field("Nome", name), Components.field("Razão social", legal)),
                Components.row(Components.field("CNPJ", document), Components.field("Telefones", phone)),
                Components.row(Components.field("E-mail", email), Components.field("Site", website)),
                Components.field("Endereço", address),
                Components.row(Components.field("Caminho da logomarca", logo),
                        Components.button("Selecionar imagem", () -> FilePicker.chooseImage()
                                .ifPresent(chosen -> logo.set(java.nio.file.Path.of(chosen).toString())))),
                Components.compactField("IPI padrão (%)", ipi, 150),
                Components.button("Salvar configurações", this::save));
    }

    private void save() {
        try {
            BigDecimal rate = Store.decimal(ipi.get());
            if (name.get().isBlank() || rate.signum() < 0) throw new IllegalArgumentException();
            Store.Company c = app.store.data.company;
            c.name = name.get().trim(); c.legalName = legal.get().trim(); c.document = document.get().trim();
            c.phone = phone.get().trim(); c.email = email.get().trim(); c.website = website.get().trim();
            c.address = address.get().trim(); c.logo = logo.get().trim(); c.ipiPercent = rate;
            if (app.save()) AppController.alert("Configurações salvas.");
        } catch (Exception error) { AppController.alert("Informe nome e IPI válido."); }
    }
}
