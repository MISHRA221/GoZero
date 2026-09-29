package com.gozero.ingestion.service;

import com.gozero.ingestion.domain.Product;
import com.gozero.ingestion.domain.Review;
import com.gozero.ingestion.repository.ProductRepository;
import com.gozero.ingestion.repository.ReviewRepository;
import com.gozero.ingestion.scraper.BigBasketScraper;
import com.gozero.ingestion.scraper.ScrapedProduct;
import com.gozero.ingestion.scraper.ScrapedReview;
import com.gozero.ingestion.scraper.ScraperProperties;
import com.gozero.ingestion.seed.SeedDataLoader;
import com.gozero.ingestion.seed.SeedDataLoader.SeedProduct;
import com.gozero.ingestion.seed.SeedDataLoader.SeedReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class IngestionService {

    public static final String SOURCE_LIVE = "BIGBASKET_LIVE_SCRAPE";
    public static final String SOURCE_SEED = "SEED_DATASET";

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final BigBasketScraper scraper;
    private final ScraperProperties props;
    private final SeedDataLoader seed;
    private final ProductRepository products;
    private final ReviewRepository reviews;
    private final TransactionTemplate tx;

    private volatile RefreshResult lastRefresh;

    public IngestionService(BigBasketScraper scraper, ScraperProperties props, SeedDataLoader seed,
                            ProductRepository products, ReviewRepository reviews, TransactionTemplate tx) {
        this.scraper = scraper;
        this.props = props;
        this.seed = seed;
        this.products = products;
        this.reviews = reviews;
        this.tx = tx;
    }

    public synchronized RefreshResult refresh() {
        List<ScrapedProduct> scraped = List.of();
        String fallbackReason;
        if (!props.enabled()) {
            fallbackReason = "Live scraping disabled by configuration (SCRAPER_ENABLED=false)";
        } else {
            try {
                scraped = scraper.scrape();
                fallbackReason = scraped.size() >= props.minLiveProducts() ? null
                        : "Scraper returned " + scraped.size() + " products (minimum " + props.minLiveProducts()
                        + ") - page layout changed or content is rendered client-side";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fallbackReason = "Live scrape interrupted";
            } catch (Exception e) {
                fallbackReason = "Live scrape failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        boolean live = fallbackReason == null;
        if (!live) {
            log.warn("Using SEED dataset. Reason: {}", fallbackReason);
        }
        List<ScrapedProduct> liveProducts = scraped;
        int[] counts = tx.execute(status -> {
            reviews.deleteAllInBatch();
            products.deleteAllInBatch();
            return live ? persistLive(liveProducts) : persistSeed();
        });

        lastRefresh = new RefreshResult(live, live ? SOURCE_LIVE : SOURCE_SEED,
                counts[0], counts[1], counts[2], fallbackReason, Instant.now());
        log.info("Ingestion refresh complete: {}", lastRefresh);
        return lastRefresh;
    }

    public RefreshResult lastRefresh() {
        return lastRefresh;
    }

    private int[] persistLive(List<ScrapedProduct> scraped) {
        Instant now = Instant.now();
        List<Product> withoutReviews = new ArrayList<>();
        int reviewCount = 0;
        for (ScrapedProduct s : scraped) {
            Product p = new Product();
            p.setSku("BB-" + (s.externalId() != null ? s.externalId() : Integer.toHexString(s.name().hashCode())));
            p.setName(s.name());
            p.setFlavor(s.flavor());
            p.setPackSize(s.packSize());
            p.setPrice(s.price());
            p.setMrp(s.mrp() != null ? s.mrp() : s.price());
            p.setRating(s.rating());
            p.setRatingCount(s.ratingCount());
            p.setProductUrl(s.url());
            p.setSource(SOURCE_LIVE);
            p.setLiveData(true);
            p.setIngestedAt(now);
            products.save(p);

            if (s.reviews() == null || s.reviews().isEmpty()) {
                withoutReviews.add(p);
            } else {
                for (ScrapedReview r : s.reviews()) {
                    reviews.save(review(p.getId(), r.author(), r.rating(), r.text(), true, now));
                    reviewCount++;
                }
            }
        }
        int liveReviews = reviewCount;
        // BigBasket loads most reviews via a separate XHR; products without scraped reviews get flagged seed reviews.
        reviewCount += attachSeedReviews(withoutReviews, now);
        return new int[] {scraped.size(), reviewCount, liveReviews};
    }

    private int[] persistSeed() {
        Instant now = Instant.now();
        List<Product> saved = new ArrayList<>();
        for (SeedProduct s : seed.products()) {
            Product p = new Product();
            p.setSku(s.sku());
            p.setName(s.name());
            p.setFlavor(s.flavor());
            p.setPackSize(s.packSize());
            p.setPrice(s.price());
            p.setMrp(s.mrp());
            p.setRating(s.rating());
            p.setRatingCount(s.ratingCount());
            p.setSource(SOURCE_SEED);
            p.setLiveData(false);
            p.setIngestedAt(now);
            saved.add(products.save(p));
        }
        int reviewCount = attachSeedReviews(saved, now);
        return new int[] {saved.size(), reviewCount, 0};
    }

    /** Spreads each flavor's seed reviews round-robin across that flavor's SKUs so no review is duplicated. */
    private int attachSeedReviews(List<Product> targets, Instant now) {
        Map<String, List<Product>> byFlavor = new LinkedHashMap<>();
        for (Product p : targets) {
            String key = p.getFlavor() == null ? "" : p.getFlavor().toLowerCase(Locale.ROOT);
            byFlavor.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }
        int count = 0;
        for (List<Product> group : byFlavor.values()) {
            List<SeedReview> seedReviews = seed.reviewsForFlavor(group.get(0).getFlavor());
            for (int i = 0; i < seedReviews.size(); i++) {
                SeedReview r = seedReviews.get(i);
                Product target = group.get(i % group.size());
                reviews.save(review(target.getId(), r.author(), r.rating(), r.text(), false, now));
                count++;
            }
        }
        return count;
    }

    private static Review review(Long productId, String author, Integer rating, String text, boolean live, Instant now) {
        Review r = new Review();
        r.setProductId(productId);
        r.setAuthor(author);
        r.setRating(rating);
        r.setText(text.length() > 2000 ? text.substring(0, 2000) : text);
        r.setLiveData(live);
        r.setIngestedAt(now);
        return r;
    }
}
