package minastex_pedidos;

import megalodonte.base.Redirect;
import megalodonte.base.UI;
import megalodonte.base.async.Async;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import megalodonte.base.theme.ThemeManager;
import megalodonte.components.Card;
import megalodonte.components.Clickable;
import megalodonte.components.AsyncImage;
import megalodonte.components.StateView;
import megalodonte.components.Text;
import megalodonte.components.TextFlow;
import megalodonte.components.layout_components.Column;
import megalodonte.components.layout_components.Container;
import megalodonte.components.layout_components.FlowRow;
import megalodonte.props.CardProps;
import megalodonte.props.ColumnProps;
import megalodonte.props.ContainerProps;
import megalodonte.props.FlowRowProps;
import megalodonte.props.ImageProps;
import megalodonte.props.TextProps;
import megalodonte.props.TextTone;
import megalodonte.v2.Show;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/** Optional institutional showcase; an unavailable feed never blocks order management. */
final class HomeAds implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(HomeAds.class);
    private final State<List<AdsFeed.Ad>> ads = State.of(List.of());
    private volatile boolean closed;

    HomeAds() {
        Async.Run(() -> {
            try {
                var loaded = new AdsFeed().fetch();
                UI.runOnUi(() -> { if (!closed) ads.set(loaded); });
            } catch (Exception error) {
                log.debug("Vitrine institucional indisponível", error);
            }
        });
    }

    Component highlights() {
        return Show.when(ads.map(values -> !values.isEmpty()), () ->
                new StateView<>(ads, values -> section(
                        values.stream().sorted(Comparator.comparing(AdsFeed.Ad::featured).reversed())
                                .limit(2).toList(),
                        "Soluções para a Minastex",
                        "Ideias selecionadas para ampliar a automação da empresa.")));
    }

    Component catalog() {
        return Show.when(ads.map(values -> !values.isEmpty()), () ->
                new StateView<>(ads, values -> section(values,
                        "Tecnologia sob medida para a empresa",
                        "Conheça outras formas de automatizar processos, integrar equipes e atender clientes.")));
    }

    private static Component section(List<AdsFeed.Ad> values, String title, String subtitle) {
        if (values.isEmpty()) return new Column();
        return new Column(new ColumnProps().spacingOf(10).fillWidth()).children(
                new Text(title, new TextProps().fontSize(ThemeManager.theme().typography().subtitle()).bold()),
                new Container(new ContainerProps().paddingTop(0).paddingRight(0)
                        .paddingDown(8).paddingLeft(0)).children(
                        new TextFlow(new Text(subtitle, new TextProps().tone(TextTone.SECONDARY)))),
                new FlowRow(new FlowRowProps().spacingOf(12).verticalSpacing(12).fillWidth())
                        .items(values.stream().map(HomeAds::card).toList()));
    }

    private static Component card(AdsFeed.Ad ad) {
        if (ad.bannerType().equals("square")) {
            var square = new Card(
                    new AsyncImage(ad.image().toString(),
                            new ImageProps().width(300).height(300).preserveRatio(true)),
                    new CardProps().width(300).height(300).paddingAll(0)
                            .borderWidth(ad.featured() ? 1 : 0)
                            .borderColor(ad.featured() ? ThemeManager.theme().colors().primary() : null));
            return new Clickable(square).onClick(() -> Redirect.to(ad.redirect().toString()));
        }
        var content = new Column(new ColumnProps().spacingOf(8).width(300).paddingAll(12)).children(
                new AsyncImage(ad.image().toString(), new ImageProps().width(276).height(138).preserveRatio(true)),
                new TextFlow(new Text(ad.title(), new TextProps().bold())),
                new TextFlow(new Text(ad.description(), new TextProps().tone(TextTone.SECONDARY))));
        var card = new Card(content, new CardProps().width(300)
                .borderWidth(ad.featured() ? 1 : 0)
                .borderColor(ad.featured() ? ThemeManager.theme().colors().primary() : null));
        return new Clickable(card).onClick(() -> Redirect.to(ad.redirect().toString()));
    }

    @Override public void close() {
        closed = true;
        ads.set(List.of());
    }
}
