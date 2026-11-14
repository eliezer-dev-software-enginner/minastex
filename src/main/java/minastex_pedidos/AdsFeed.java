package minastex_pedidos;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Reads Minastex campaigns from the public portfolio page without running its scripts. */
final class AdsFeed {
    static final URI PAGE = URI.create("https://eliezer-dev.vercel.app/anuncios");
    private static final int MAX_PAGE_SIZE = 1_000_000;
    private static volatile List<Ad> cachedAds;
    private final HttpClient client;

    record Ad(String title, String description, URI image, boolean featured, URI redirect,
              String bannerType) { }

    AdsFeed() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    AdsFeed(HttpClient client) { this.client = client; }

    List<Ad> fetch() throws IOException, InterruptedException {
        List<Ad> cached = cachedAds;
        if (cached != null) return cached;
        synchronized (AdsFeed.class) {
            if (cachedAds != null) return cachedAds;
            var request = HttpRequest.newBuilder(PAGE).timeout(Duration.ofSeconds(8)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new IOException("Página de anúncios: HTTP " + response.statusCode());
            if (response.body().length() > MAX_PAGE_SIZE)
                throw new IOException("Página de anúncios maior que o esperado");
            cachedAds = parse(response.body(), PAGE);
            return cachedAds;
        }
    }

    static List<Ad> parse(String html, URI page) {
        var document = Jsoup.parse(html, page.toString());
        var ads = new ArrayList<Ad>();
        for (var element : document.select("span[data-audiences]")) {
            if (!supports(element.attr("data-audiences"), "minastex-pedidos")) continue;
            add(ads, page, element.attr("data-title"), element.attr("data-description"),
                    element.attr("data-image"), "true".equalsIgnoreCase(element.attr("data-urgent")),
                    element.attr("data-redirect"), element.attr("data-banner-type"));
        }
        if (!ads.isEmpty()) return List.copyOf(ads);

        for (var element : document.select("span[data-minastex-ad=true]")) {
            add(ads, page, element.attr("data-title"), element.attr("data-description"),
                    element.attr("data-image"), "true".equalsIgnoreCase(element.attr("data-urgent")),
                    element.attr("data-redirect"), element.attr("data-banner-type"));
        }
        if (!ads.isEmpty()) return List.copyOf(ads);

        // Compatibility while the targeted feed is being published.
        var legacy = document.select("span[data-plics-ad=true]");
        for (int index = 0; index < legacy.size(); index++) {
            var element = legacy.get(index);
            boolean systems = "true".equalsIgnoreCase(element.attr("data-urgent"));
            add(ads, page,
                    systems ? "Mais sistemas sob medida para a Minastex"
                            : "Automatize atendimentos e rotinas da empresa",
                    systems ? "Transforme outros processos em aplicações simples, rápidas e integradas."
                            : "Bots e integrações podem reduzir tarefas repetitivas e aproximar a equipe dos clientes.",
                    element.attr("data-image"), systems, element.attr("data-redirect"), "banner");
        }
        return List.copyOf(ads);
    }

    private static boolean supports(String audiences, String application) {
        return java.util.Arrays.stream(audiences.split(","))
                .map(String::strip)
                .anyMatch(application::equals);
    }

    private static void add(List<Ad> ads, URI page, String title, String description,
                            String image, boolean featured, String redirect, String bannerType) {
        if (image == null || image.isBlank()) return;
        try {
            URI assetBase = page.resolve(page.getPath().endsWith("/") ? page.getPath() : page.getPath() + "/");
            URI imageUrl = assetBase.resolve(image.strip());
            if (!safe(imageUrl)) return;
            URI destination = redirect == null || redirect.isBlank() || redirect.equals("undefined")
                    ? page.resolve("/") : page.resolve(redirect.strip());
            if (!safe(destination)) destination = page.resolve("/");
            ads.add(new Ad(title == null || title.isBlank() ? "Conheça esta solução" : title.strip(),
                    description == null ? "" : description.strip(), imageUrl, featured, destination,
                    "square".equalsIgnoreCase(bannerType) ? "square" : "banner"));
        } catch (IllegalArgumentException ignored) { }
    }

    private static boolean safe(URI uri) {
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null;
    }
}
