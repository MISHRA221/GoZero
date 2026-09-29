package com.gozero.ingestion.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bundled, clearly-labelled fallback dataset used when live scraping is blocked or disabled.
 * Values are illustrative, not Go Zero's actual catalogue numbers.
 */
@Component
public class SeedDataLoader {

    public record SeedProduct(String sku, String name, String flavor, String packSize,
                              BigDecimal price, BigDecimal mrp, Double rating, Integer ratingCount) {
    }

    public record SeedReview(String author, Integer rating, String text) {
    }

    private final List<SeedProduct> products;
    private final Map<String, List<SeedReview>> reviewsByFlavor;

    public SeedDataLoader(ObjectMapper mapper) throws IOException {
        try (InputStream in = new ClassPathResource("seed/products.json").getInputStream()) {
            this.products = mapper.readValue(in, new TypeReference<List<SeedProduct>>() { });
        }
        try (InputStream in = new ClassPathResource("seed/reviews.json").getInputStream()) {
            this.reviewsByFlavor = mapper.readValue(in, new TypeReference<Map<String, List<SeedReview>>>() { });
        }
    }

    public List<SeedProduct> products() {
        return products;
    }

    /** Exact (case-insensitive) flavor match first, then a containment match for scraped titles. */
    public List<SeedReview> reviewsForFlavor(String flavor) {
        if (flavor == null) {
            return List.of();
        }
        String f = flavor.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<SeedReview>> e : reviewsByFlavor.entrySet()) {
            if (e.getKey().equalsIgnoreCase(flavor)) {
                return e.getValue();
            }
        }
        for (Map.Entry<String, List<SeedReview>> e : reviewsByFlavor.entrySet()) {
            String k = e.getKey().toLowerCase(Locale.ROOT);
            if (f.contains(k) || k.contains(f)) {
                return e.getValue();
            }
        }
        return List.of();
    }
}
