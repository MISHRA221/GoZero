package com.gozero.reconciliation.simulation;

import com.gozero.reconciliation.client.IngestionDtos.Product;
import com.gozero.reconciliation.config.ReconciliationProperties;
import com.gozero.reconciliation.domain.AlertType;
import com.gozero.reconciliation.domain.Platform;
import com.gozero.reconciliation.domain.PlatformListing;
import com.gozero.reconciliation.domain.ReconciliationAlert;
import com.gozero.reconciliation.engine.AlertEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformSimulatorTest {

    private static final ReconciliationProperties PROPS =
            new ReconciliationProperties(42, 5.0, 11.0, 15.0, 0.15, 0.10, 0.07, 10.0, 13.0);

    private final PlatformSimulator simulator = new PlatformSimulator(PROPS);
    private final AlertEngine engine = new AlertEngine(PROPS);

    private static List<Product> thirtySkus() {
        return IntStream.rangeClosed(1, 30)
                .mapToObj(i -> new Product((long) i, String.format("GZ-%03d", i), "Go Zero Flavor " + i, "Flavor " + i,
                        "500 ml", BigDecimal.valueOf(299), BigDecimal.valueOf(350), false))
                .toList();
    }

    @Test
    void generatesFourPlatformsPerSkuWithBigBasketAsReference() {
        List<PlatformListing> listings = simulator.simulate(thirtySkus(), Instant.now());
        assertThat(listings).hasSize(120);
        assertThat(listings).filteredOn(l -> l.getPlatform() == Platform.BIGBASKET)
                .allSatisfy(l -> {
                    assertThat(l.getPrice()).isEqualByComparingTo("299");
                    assertThat(l.getPriceSource()).isEqualTo("SEED_DATASET");
                });
        assertThat(listings).filteredOn(l -> l.getPlatform() != Platform.BIGBASKET)
                .allSatisfy(l -> assertThat(l.getPriceSource()).isEqualTo(PlatformSimulator.SIMULATED));
    }

    @Test
    void injectsAnomaliesAtConfiguredRates() {
        List<ReconciliationAlert> alerts = engine.evaluate(simulator.simulate(thirtySkus(), Instant.now()), Instant.now());

        long priceSkus = alerts.stream().filter(a -> a.getType() == AlertType.PRICE_VARIANCE)
                .map(ReconciliationAlert::getSku).distinct().count();
        long stockSkus = alerts.stream().filter(a -> a.getType() == AlertType.STOCK_MISMATCH).count();
        long gapSkus = alerts.stream().filter(a -> a.getType() == AlertType.ASSORTMENT_GAP).count();

        assertThat(priceSkus).isBetween(4L, 5L);   // ~15% of 30
        assertThat(stockSkus).isEqualTo(3L);       // ~10% of 30
        assertThat(gapSkus).isEqualTo(2L);         // ~7% of 30
    }

    @Test
    void normalJitterNeverBreachesThresholdOrMrp() {
        List<PlatformListing> listings = simulator.simulate(thirtySkus(), Instant.now());
        assertThat(listings).filteredOn(l -> l.getPrice() != null)
                .allSatisfy(l -> assertThat(l.getPrice()).isLessThanOrEqualTo(BigDecimal.valueOf(350)));
    }

    @Test
    void isDeterministicForTheSameSeed() {
        List<PlatformListing> a = simulator.simulate(thirtySkus(), Instant.now());
        List<PlatformListing> b = simulator.simulate(thirtySkus(), Instant.now());
        assertThat(a).extracting(PlatformListing::getPrice).containsExactlyElementsOf(
                b.stream().map(PlatformListing::getPrice).toList());
    }
}
