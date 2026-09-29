package com.gozero.reconciliation.simulation;

import com.gozero.reconciliation.client.IngestionDtos.Product;
import com.gozero.reconciliation.config.ReconciliationProperties;
import com.gozero.reconciliation.domain.Platform;
import com.gozero.reconciliation.domain.PlatformListing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates SIMULATED Blinkit / Zepto / Instamart listings around the BigBasket reference price.
 * Deterministic (seeded) so dashboards and tests are reproducible. Anomaly buckets are disjoint:
 * ~15% of SKUs get an 11-15% price deviation on one platform, ~10% go out of stock on 1-2 platforms,
 * ~7% are missing from one platform entirely. All other simulated prices jitter within +/-5%.
 */
public class PlatformSimulator {

    public static final String SIMULATED = "SIMULATED";
    private static final List<Platform> SIMULATED_PLATFORMS = List.of(Platform.BLINKIT, Platform.ZEPTO, Platform.INSTAMART);

    private final ReconciliationProperties props;

    public PlatformSimulator(ReconciliationProperties props) {
        this.props = props;
    }

    public List<PlatformListing> simulate(List<Product> products, Instant now) {
        List<Product> sorted = products.stream()
                .filter(p -> p.price() != null && p.price().signum() > 0)
                .sorted(Comparator.comparing(Product::sku))
                .toList();
        List<Product> shuffled = new ArrayList<>(sorted);
        Collections.shuffle(shuffled, new Random(props.randomSeed()));

        int n = shuffled.size();
        int priceCount = (int) Math.round(n * props.priceAnomalyRate());
        int stockCount = (int) Math.round(n * props.stockAnomalyRate());
        int gapCount = (int) Math.round(n * props.assortmentGapRate());
        Set<String> priceSkus = skus(shuffled, 0, priceCount);
        Set<String> stockSkus = skus(shuffled, priceCount, priceCount + stockCount);
        Set<String> gapSkus = skus(shuffled, priceCount + stockCount, priceCount + stockCount + gapCount);

        List<PlatformListing> listings = new ArrayList<>();
        for (Product p : sorted) {
            Random r = new Random(props.randomSeed() * 31 + p.sku().hashCode());
            BigDecimal ref = p.price();
            BigDecimal mrp = p.mrp() != null && p.mrp().compareTo(ref) >= 0 ? p.mrp() : ref;

            Platform priceAnomaly = priceSkus.contains(p.sku()) ? pick(r) : null;
            Platform gap = gapSkus.contains(p.sku()) ? pick(r) : null;
            Set<Platform> outOfStock = new HashSet<>();
            if (stockSkus.contains(p.sku())) {
                List<Platform> candidates = new ArrayList<>(SIMULATED_PLATFORMS);
                Collections.shuffle(candidates, r);
                outOfStock.addAll(candidates.subList(0, 1 + r.nextInt(2)));
            }

            listings.add(listing(p, Platform.BIGBASKET, ref, mrp, true, true, 0.0,
                    p.isLiveData() ? "BIGBASKET_SCRAPED" : "SEED_DATASET", now));

            for (Platform platform : SIMULATED_PLATFORMS) {
                if (platform == gap) {
                    listings.add(listing(p, platform, null, mrp, false, false, null, SIMULATED, now));
                    continue;
                }
                double pct = platform == priceAnomaly
                        ? anomalyPct(r, ref, mrp)
                        : (r.nextDouble() * 2 - 1) * props.normalJitterPct();
                BigDecimal price = ref.multiply(BigDecimal.valueOf(1 + pct / 100)).setScale(0, RoundingMode.HALF_UP);
                if (price.compareTo(mrp) > 0) {
                    price = mrp.setScale(0, RoundingMode.DOWN);
                }
                double deviation = price.subtract(ref).multiply(BigDecimal.valueOf(100))
                        .divide(ref, 2, RoundingMode.HALF_UP).doubleValue();
                listings.add(listing(p, platform, price, mrp, !outOfStock.contains(platform), true, deviation,
                        SIMULATED, now));
            }
        }
        return listings;
    }

    /** Deviation magnitude in [min, max]; goes negative when a markup would breach MRP (selling above MRP is illegal in India). */
    private double anomalyPct(Random r, BigDecimal ref, BigDecimal mrp) {
        double magnitude = props.anomalyMinPct() + r.nextDouble() * (props.anomalyMaxPct() - props.anomalyMinPct());
        boolean up = r.nextBoolean();
        BigDecimal upPrice = ref.multiply(BigDecimal.valueOf(1 + magnitude / 100));
        return up && upPrice.compareTo(mrp) <= 0 ? magnitude : -magnitude;
    }

    private static Platform pick(Random r) {
        return SIMULATED_PLATFORMS.get(r.nextInt(SIMULATED_PLATFORMS.size()));
    }

    private static Set<String> skus(List<Product> list, int from, int to) {
        int a = Math.min(from, list.size());
        int b = Math.min(to, list.size());
        return list.subList(a, b).stream().map(Product::sku).collect(Collectors.toSet());
    }

    private static PlatformListing listing(Product p, Platform platform, BigDecimal price, BigDecimal mrp,
                                           boolean inStock, boolean listed, Double deviation, String source, Instant now) {
        PlatformListing l = new PlatformListing();
        l.setProductId(p.id());
        l.setSku(p.sku());
        l.setProductName(p.name());
        l.setFlavor(p.flavor());
        l.setPackSize(p.packSize());
        l.setPlatform(platform);
        l.setPrice(price);
        l.setMrp(mrp);
        l.setInStock(inStock);
        l.setListed(listed);
        l.setDeviationPct(deviation);
        l.setPriceSource(source);
        l.setGeneratedAt(now);
        return l;
    }
}
