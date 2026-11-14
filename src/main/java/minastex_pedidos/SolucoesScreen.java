package minastex_pedidos;

import megalodonte.base.components.Component;
import megalodonte.base.components.ScreenComponent;
import megalodonte.components.Text;

/** Dedicated EliezerDev showcase with every campaign selected for Minastex. */
final class SolucoesScreen implements ScreenComponent {
    private final HomeAds ads;

    SolucoesScreen(HomeAds ads) { this.ads = ads; }

    @Override public Component render() {
        return Components.page("Soluções EliezerDev",
                new Text("Novas ferramentas podem ser desenvolvidas conforme os próximos desafios da Minastex."),
                ads.catalog());
    }
}
