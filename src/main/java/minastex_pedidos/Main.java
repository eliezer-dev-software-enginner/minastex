package minastex_pedidos;

import megalodonte.application.MegalodonteApp;
import megalodonte.base.theme.ThemeManager;

public final class Main {
    public static void main(String[] args) {
        ThemeManager.setTheme(new MinastexTheme());
        MegalodonteApp.appName("Minastex Pedidos");
        MegalodonteApp.appIcon("/icon.png");
        MegalodonteApp.run(context -> {
            context.javafxStage().setMinWidth(980);
            context.javafxStage().setMinHeight(650);
            context.useView(new HomeScreen(), 1260, 780);
        });
    }
}
