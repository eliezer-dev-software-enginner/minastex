package minastex_pedidos;

import disgust.icons.Feather;
import disgust.io.ButtonsPack;
import disgust.io.Tooltip;
import disgust.io.Tooltip.Position;
import megalodonte.base.components.Component;
import megalodonte.base.components.Ref;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.LineHorizontal;
import megalodonte.components.SpacerVertical;
import megalodonte.components.StateView;
import megalodonte.components.Text;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.Row;
import megalodonte.props.ButtonVariant;
import megalodonte.props.ContainerProps;
import megalodonte.props.RowProps;
import megalodonte.props.TextProps;


/** Declarative navigation shell. Each menu destination owns its own screen. */
public final class HomeScreen implements ScreenComponent {
    private final AppController app = new AppController();
    Ref<Tooltip> tooltipRef = new Ref<>();

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
                        new Tooltip("Vá para a página do criador",
                        		ButtonsPack.OutlinedButton("Criado por EliezerDev", ButtonVariant.SUCCESS, "https://eliezer-dev.vercel.app/")
                        		).position(Position.RIGHT).enterDelay(200)
                        .ref(tooltipRef)),
                new StateView<>(app.screen, screen -> screen.render())
        );
    }
    
    	@Override
    	public void onDestroy() {
    		tooltipRef.current().close();
    		IO.print("tooltip killed");
    	}
}
