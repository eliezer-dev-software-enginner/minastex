package minastex_pedidos;

import megalodonte.application.MegalodonteApp;
import megalodonte.application.MegalodonteApplication;
import megalodonte.base.components.Ref;
import megalodonte.base.components.ScreenComponent;
import megalodonte.base.theme.ThemeManager;

public final class Main {
    public static final class AppHost extends MegalodonteApplication {}
    private static final Ref<ScreenComponent> rootScreen = new Ref<>(new HomeScreen());

    static void main(String[] args) {
        ThemeManager.setTheme(new MinastexTheme());
        MegalodonteApp.appName("Minastex Pedidos");
        MegalodonteApp.appIcon("/icon.png");
        
        MegalodonteApp.run(AppHost.class, args, context -> {
            context.javafxStage().setMinWidth(980);
            context.javafxStage().setMinHeight(650);
            context.useView(rootScreen.current(), 1260, 780);
        }, ev->{
        	if(ev == MegalodonteApp.Event.CloseRequest) {
        		rootScreen.current().onDestroy();
        	}
        });
        
       
    }
}
