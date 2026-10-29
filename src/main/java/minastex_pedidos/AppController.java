package minastex_pedidos;

import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.components.DesktopDialogs;
import megalodonte.components.FilePicker;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Navigation and application services; screens own only their presentation state. */
final class AppController {
    final Store store;
    final State<ScreenComponent> screen = State.of(null);

    AppController() { this(openDefaultStore()); }

    AppController(Store store) {
        this.store = store;
        home();
    }

    private static Store openDefaultStore() {
        try { return new Store(Store.defaultFile()); }
        catch (Exception error) { throw new IllegalStateException("Não foi possível abrir os pedidos", error); }
    }

    void home() { screen.set(new InicioScreen(this)); }
    void history() { screen.set(new HistoricoScreen(this)); }
    void order(Store.Order existing) { screen.set(new PedidoScreen(this, existing)); }
    void registry(String kind) { screen.set(new CadastroScreen(this, kind)); }
    void company() { screen.set(new EmpresaScreen(this)); }

    boolean save() {
        try { store.save(); return true; }
        catch (Exception error) { alert("Falha ao salvar: " + error.getMessage()); return false; }
    }

    void export(Store.Order order) {
        FilePicker.saveFile( "Exportar pedido", "pedido-minastex-" + order.number + ".pdf", "PDF", List.of("*.pdf"))
                .ifPresent(file -> {
                    try { PdfExporter.export(order, store.data.company, file); alert("PDF salvo em " + file); }
                    catch (Exception error) { alert("Falha no PDF: " + error.getMessage()); }
                });
    }

    void preview(Store.Order order) {
        try {
            Path file = Files.createTempFile("minastex-" + order.number + "-", ".pdf");
            PdfExporter.export(order, store.data.company, file);
            Desktop.getDesktop().open(file.toFile());
        } catch (Exception error) { alert("Falha na prévia: " + error.getMessage()); }
    }

    static void alert(String message) { DesktopDialogs.information(message); }
    static boolean confirm(String message) { return DesktopDialogs.confirm(message); }
}
