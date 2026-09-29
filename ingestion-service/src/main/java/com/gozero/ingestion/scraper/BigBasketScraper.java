package com.gozero.ingestion.scraper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort scraper for Go Zero's public BigBasket brand page.
 * Polite by design: robots.txt is checked first, requests are sequential with a fixed delay,
 * and the number of product pages is capped.
 */
@Component
public class BigBasketScraper {

    private static final Logger log = LoggerFactory.getLogger(BigBasketScraper.class);
    private static final Pattern PD_ID = Pattern.compile("/pd/(\\d+)");

    private final ScraperProperties props;
    private final ObjectMapper mapper;

    public BigBasketScraper(ScraperProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    public List<ScrapedProduct> scrape() throws IOException, InterruptedException {
        RobotsTxtPolicy robots = fetchRobots();
        if (!robots.isAllowed(props.brandPath())) {
            throw new ScrapeBlockedException("robots.txt disallows " + props.brandPath());
        }

        String listingUrl = props.baseUrl() + props.brandPath();
        log.info("Fetching BigBasket listing {}", listingUrl);
        Document listing = fetch(listingUrl);

        List<ScrapedProduct> fromListing = extractFromNextData(listing);
        List<String> links = fromListing.isEmpty() ? extractProductLinks(listing) : List.of();
        int total = fromListing.isEmpty() ? links.size() : fromListing.size();
        int limit = Math.min(props.maxProducts(), total);
        log.info("Listing yielded {} embedded products and {} product links; scraping up to {}",
                fromListing.size(), links.size(), limit);

        List<ScrapedProduct> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            ScrapedProduct base = fromListing.isEmpty() ? null : fromListing.get(i);
            String url = base != null ? base.url() : links.get(i);
            ScrapedProduct detail = null;
            if (url != null && robots.isAllowed(pathOf(url))) {
                Thread.sleep(props.delayMs());
                try {
                    detail = parseProductPage(fetch(url), url);
                } catch (IOException e) {
                    log.debug("Product page {} failed: {}", url, e.getMessage());
                }
            }
            ScrapedProduct merged = merge(base, detail);
            if (merged != null && merged.name() != null && merged.price() != null) {
                result.add(merged);
            }
        }
        return result;
    }

    private RobotsTxtPolicy fetchRobots() throws IOException {
        Connection.Response res = Jsoup.connect(props.baseUrl() + "/robots.txt")
                .userAgent(props.userAgent())
                .timeout(props.timeoutMs())
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .execute();
        if (res.statusCode() == 404) {
            return RobotsTxtPolicy.allowAll();
        }
        if (res.statusCode() >= 400) {
            throw new ScrapeBlockedException("robots.txt returned HTTP " + res.statusCode() + " - not scraping");
        }
        return RobotsTxtPolicy.parse(res.body());
    }

    private Document fetch(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(props.userAgent())
                .timeout(props.timeoutMs())
                .header("Accept-Language", "en-IN,en;q=0.9")
                .followRedirects(true)
                .get();
    }

    /** BigBasket is a Next.js app; product tiles are embedded as JSON in __NEXT_DATA__ (schema may change). */
    List<ScrapedProduct> extractFromNextData(Document doc) {
        Element script = doc.selectFirst("script#__NEXT_DATA__");
        if (script == null) {
            return List.of();
        }
        JsonNode root;
        try {
            root = mapper.readTree(script.data());
        } catch (JsonProcessingException e) {
            return List.of();
        }
        List<ScrapedProduct> out = new ArrayList<>();
        collectProducts(root, out, new HashSet<>());
        return out;
    }

    private void collectProducts(JsonNode node, List<ScrapedProduct> out, Set<String> seen) {
        if (node.isObject() && node.hasNonNull("desc") && node.has("pricing")) {
            ScrapedProduct p = fromNextDataNode(node);
            if (isGoZero(p.name()) && seen.add(p.externalId() != null ? p.externalId() : p.name())) {
                out.add(p);
            }
            return;
        }
        if (node.isContainerNode()) {
            node.forEach(child -> collectProducts(child, out, seen));
        }
    }

    private ScrapedProduct fromNextDataNode(JsonNode node) {
        String name = node.path("desc").asText();
        String brand = node.path("brand").path("name").asText("");
        if (!brand.isBlank() && !name.toLowerCase(Locale.ROOT).contains(brand.toLowerCase(Locale.ROOT))) {
            name = brand + " " + name;
        }
        JsonNode discount = node.path("pricing").path("discount");
        BigDecimal mrp = decimal(discount.path("mrp"));
        BigDecimal sp = decimal(discount.path("prim_price").path("sp"));
        JsonNode ratingInfo = node.path("rating_info");
        String pack = node.hasNonNull("w") ? node.path("w").asText() : ProductNameParser.packSize(name);
        return new ScrapedProduct(
                node.path("id").asText(null),
                name,
                ProductNameParser.flavor(name),
                pack,
                sp != null ? sp : mrp,
                mrp,
                dbl(ratingInfo.path("avg_rating")),
                integer(ratingInfo.path("rating_count")),
                absolutize(node.path("absolute_url").asText(null)),
                List.of());
    }

    List<String> extractProductLinks(Document doc) {
        Set<String> links = new LinkedHashSet<>();
        for (Element a : doc.select("a[href*=/pd/]")) {
            String href = a.absUrl("href");
            if (!href.isBlank()) {
                links.add(href.split("\\?")[0]);
            }
        }
        return new ArrayList<>(links);
    }

    /** Product detail pages expose schema.org Product JSON-LD (name, offer price, aggregate rating, reviews). */
    ScrapedProduct parseProductPage(Document doc, String url) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            JsonNode json;
            try {
                json = mapper.readTree(script.data());
            } catch (JsonProcessingException e) {
                continue;
            }
            JsonNode product = findByType(json, "Product");
            if (product == null) {
                continue;
            }
            String name = product.path("name").asText(null);
            JsonNode offers = product.path("offers");
            if (offers.isArray() && !offers.isEmpty()) {
                offers = offers.get(0);
            }
            JsonNode agg = product.path("aggregateRating");
            Integer count = integer(agg.path("ratingCount"));
            if (count == null) {
                count = integer(agg.path("reviewCount"));
            }
            return new ScrapedProduct(
                    externalIdFromUrl(url),
                    name,
                    ProductNameParser.flavor(name),
                    ProductNameParser.packSize(name),
                    decimal(offers.path("price")),
                    null,
                    dbl(agg.path("ratingValue")),
                    count,
                    url,
                    reviews(product.path("review")));
        }
        return null;
    }

    private List<ScrapedReview> reviews(JsonNode node) {
        List<ScrapedReview> out = new ArrayList<>();
        List<JsonNode> items = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(items::add);
        } else if (node.isObject()) {
            items.add(node);
        }
        for (JsonNode r : items) {
            String text = r.path("reviewBody").asText(r.path("description").asText(""));
            if (text.isBlank()) {
                continue;
            }
            JsonNode author = r.path("author");
            String authorName = author.isObject() ? author.path("name").asText(null) : author.asText(null);
            Double stars = dbl(r.path("reviewRating").path("ratingValue"));
            out.add(new ScrapedReview(authorName, stars == null ? null : (int) Math.round(stars), text.trim()));
        }
        return out;
    }

    private static JsonNode findByType(JsonNode node, String type) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode hit = findByType(child, type);
                if (hit != null) {
                    return hit;
                }
            }
            return null;
        }
        if (!node.isObject()) {
            return null;
        }
        JsonNode t = node.path("@type");
        if (type.equals(t.asText()) || (t.isArray() && t.toString().contains("\"" + type + "\""))) {
            return node;
        }
        return node.has("@graph") ? findByType(node.get("@graph"), type) : null;
    }

    private static ScrapedProduct merge(ScrapedProduct base, ScrapedProduct detail) {
        if (base == null) {
            return detail;
        }
        if (detail == null) {
            return base;
        }
        return new ScrapedProduct(
                first(base.externalId(), detail.externalId()),
                first(base.name(), detail.name()),
                first(base.flavor(), detail.flavor()),
                first(base.packSize(), detail.packSize()),
                first(base.price(), detail.price()),
                first(base.mrp(), detail.mrp()),
                first(detail.rating(), base.rating()),
                first(detail.ratingCount(), base.ratingCount()),
                first(base.url(), detail.url()),
                detail.reviews());
    }

    private static <T> T first(T a, T b) {
        return a != null ? a : b;
    }

    private static boolean isGoZero(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return n.contains("go zero") || n.contains("gozero");
    }

    private String absolutize(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.startsWith("http") ? url : props.baseUrl() + (url.startsWith("/") ? url : "/" + url);
    }

    private static String pathOf(String url) {
        try {
            return URI.create(url).getRawPath();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }

    private static String externalIdFromUrl(String url) {
        Matcher m = PD_ID.matcher(url == null ? "" : url);
        return m.find() ? m.group(1) : null;
    }

    private static BigDecimal decimal(JsonNode n) {
        if (n.isNumber()) {
            return n.decimalValue();
        }
        if (n.isTextual()) {
            String digits = n.asText().replaceAll("[^0-9.]", "");
            try {
                return digits.isEmpty() ? null : new BigDecimal(digits);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Double dbl(JsonNode n) {
        BigDecimal d = decimal(n);
        return d == null ? null : d.doubleValue();
    }

    private static Integer integer(JsonNode n) {
        BigDecimal d = decimal(n);
        return d == null ? null : d.intValue();
    }
}
