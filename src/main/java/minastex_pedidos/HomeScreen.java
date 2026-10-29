package minastex_pedidos;

import disgust.icons.Feather;
import megalodonte.base.Redirect;
import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Clickable;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.StateView;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;

import java.awt.Desktop;
import java.net.URI;

/** Declarative navigation shell. Each menu destination owns its own screen. */
public final class HomeScreen implements ScreenComponent {
    private final AppController app = new AppController();

    @Override public Component render() {
        return new Row(new RowProps().fillHeight().bgColor(ThemeManager.theme().colors().background())).children(
                new Container(new ContainerProps().themeSurface().spacingOf(8).width(220).fillHeight().paddingAll(16)).children(
                        new Text("MINASTEX\nPEDIDOS", new TextProps().bold()),
                        new LineHorizontal(),
                        Components.nav("Início", Feather.HOME, app::home),
                        Components.nav("Novo pedido", Feather.FILE_PLUS, () -> app.order(null)),
                        Components.nav("Histórico", Feather.LIST, app::history),
                        Components.nav("Clientes", Feather.USERS, () -> app.registry("Clientes")),
                        Components.nav("Acabamentos", Feather.LAYERS, () -> app.registry("Acabamentos")),
                        Components.nav("Contas bancárias", Feather.CREDIT_CARD, () -> app.registry("Contas bancárias")),
                        Components.nav("Empresa", Feather.SETTINGS, app::company),
                        new SpacerVertical().fill(),
                        new Clickable(new Text("Desenvolvido por EliezerDev")).onClick(() -> {
                            //NÃO USAR DESSA FORMA, USAR O REDIRECT NATIVO DO MEGALODONTE (salvar nas skills)
//                            try { Desktop.getDesktop().browse(URI.create("https://eliezer-dev.vercel.app/")); }
//                            catch (Exception error) { AppController.alert("Não foi possível abrir o portfólio: " + error.getMessage()); }

                            //REDIRECT NATIVO
                            Redirect.to("https://eliezer-dev.vercel.app/", error->AppController.alert("Não foi possível abrir o portfólio: " + error.getMessage()));
                        })
                ),
                new StateView<>(app.screen, screen -> screen.render())
        );
    }
}
