package minastex_pedidos;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdsFeedTest {
    private static final URI PAGE = URI.create("https://eliezer-dev.vercel.app/anuncios");

    @Test void readsOnlyMinastexCampaignsWhenTargetedContentExists() {
        String html = "<span data-plics-ad='true' data-title='Plics' data-image='/plics.png'></span>" +
                "<span data-minastex-ad='true' data-title='Sob medida' data-description='Para a empresa' " +
                "data-image='/minastex.png' data-urgent='true' data-redirect='https://example.com/contact'></span>";
        var ads = AdsFeed.parse(html, PAGE);
        assertEquals(1, ads.size());
        assertEquals("Sob medida", ads.getFirst().title());
        assertEquals("Para a empresa", ads.getFirst().description());
        assertTrue(ads.getFirst().featured());
        assertEquals("https://eliezer-dev.vercel.app/minastex.png", ads.getFirst().image().toString());
    }

    @Test void temporarilyFallsBackToPublishedPlicsCampaigns() {
        var ads = AdsFeed.parse("<span data-plics-ad='true' data-title='Automação' " +
                "data-image='./assets/banner.png' data-urgent='false' data-redirect='undefined'></span>", PAGE);
        assertEquals(1, ads.size());
        assertEquals("Automatize atendimentos e rotinas da empresa", ads.getFirst().title());
        assertFalse(ads.getFirst().featured());
        assertEquals("https://eliezer-dev.vercel.app/", ads.getFirst().redirect().toString());
    }
}
