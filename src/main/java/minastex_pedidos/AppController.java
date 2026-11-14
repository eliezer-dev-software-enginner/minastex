package minastex_pedidos;

import megalodonte.base.components.ScreenComponent;
import megalodonte.base.state.State;
import megalodonte.base.UI;
import megalodonte.components.DesktopDialogs;
import megalodonte.components.FilePicker;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Navigation and application services; screens own only their presentation state. */
final class AppController {
    final Store store;
    final HomeAds ads;
    final State<ScreenComponent> screen = State.of(null);

    AppController() { this(openDefaultStore()); }

    AppController(Store store) {
        this.store = store;
        this.ads = new HomeAds();
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
    void solutions() { screen.set(new SolucoesScreen(ads)); }

    void close() { ads.close(); }

    boolean save() {
        try { store.save(); return true; }
        catch (Exception error) { alert("Falha ao salvar: " + error.getMessage()); return false; }
    }

    void export(Store.Order order) {
        FilePicker.saveFile( "Exportar pedido", "pedido-minastex-" + order.number + ".pdf", "PDF", List.of("*.pdf"))
                .ifPresent(file -> {
                    Store.Order snapshot = Store.copy(order);
                    Store.Company company = Store.copy(store.data.company);
                    Thread.ofVirtual().name("minastex-export-pdf").start(() -> {
                        try {
                            OrderPdf.write(snapshot, company, file);
                            System.out.println("PDF salvo em " + file);
                            UI.runOnUi(() -> alert("PDF salvo em " + file));
                        } catch (Exception error) {
                            error.printStackTrace();
                            UI.runOnUi(() -> alert("Falha no PDF: " + error.getMessage()));
                        }
                    });
                });
    }

    void preview(Store.Order order) {
        Store.Order snapshot = Store.copy(order);
        Store.Company company = Store.copy(store.data.company);
        Thread.ofVirtual().name("minastex-preview-pdf").start(() -> {
            try {
                Path file = Files.createTempFile("minastex-" + snapshot.number + "-", ".pdf");
                OrderPdf.write(snapshot, company, file);
                openPdf(file);
                System.out.println("Prévia do PDF aberta: " + file);
            } catch (Exception error) {
                error.printStackTrace();
                UI.runOnUi(() -> alert("Falha na prévia: " + error.getMessage()));
            }
        });
    }

    private static void openPdf(Path file) throws IOException {
        if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("linux")) {
            Process opener = new ProcessBuilder("xdg-open", file.toString())
                    .redirectErrorStream(true).start();
            try {
                if (opener.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) && opener.exitValue() != 0)
                    throw new IOException("Não foi possível abrir o visualizador de PDF (xdg-open: " + opener.exitValue() + "). Arquivo: " + file);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("A abertura do PDF foi interrompida. Arquivo: " + file, error);
            }
        } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(file.toFile());
        } else throw new IOException("Nenhum visualizador de PDF disponível. Arquivo: " + file);
    }

    static void alert(String message) { DesktopDialogs.information(message); }
    static boolean confirm(String message) { return DesktopDialogs.confirm(message); }
}
