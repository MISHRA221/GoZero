package com.gozero.reconciliation.service;

import com.gozero.reconciliation.client.IngestionClient;
import com.gozero.reconciliation.client.IngestionDtos.Product;
import com.gozero.reconciliation.domain.AlertType;
import com.gozero.reconciliation.domain.Platform;
import com.gozero.reconciliation.domain.PlatformListing;
import com.gozero.reconciliation.domain.PlatformListingRepository;
import com.gozero.reconciliation.domain.ReconciliationAlert;
import com.gozero.reconciliation.domain.ReconciliationAlertRepository;
import com.gozero.reconciliation.domain.Severity;
import com.gozero.reconciliation.engine.AlertEngine;
import com.gozero.reconciliation.simulation.PlatformSimulator;
import com.gozero.reconciliation.web.AlertDto;
import com.gozero.reconciliation.web.ApiEnvelope;
import com.gozero.reconciliation.web.ListingDto;
import com.gozero.reconciliation.web.SkuReconciliationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final IngestionClient ingestion;
    private final PlatformSimulator simulator;
    private final AlertEngine engine;
    private final PlatformListingRepository listings;
    private final ReconciliationAlertRepository alerts;
    private final TransactionTemplate tx;

    public ReconciliationService(IngestionClient ingestion, PlatformSimulator simulator, AlertEngine engine,
                                 PlatformListingRepository listings, ReconciliationAlertRepository alerts,
                                 TransactionTemplate tx) {
        this.ingestion = ingestion;
        this.simulator = simulator;
        this.engine = engine;
        this.listings = listings;
        this.alerts = alerts;
        this.tx = tx;
    }

    public synchronized RunResult run() {
        List<Product> products = ingestion.products();
        Instant now = Instant.now();
        List<PlatformListing> generated = simulator.simulate(products, now);
        List<ReconciliationAlert> raised = engine.evaluate(generated, now);
        tx.executeWithoutResult(status -> {
            alerts.deleteAllInBatch();
            listings.deleteAllInBatch();
            listings.saveAll(generated);
            alerts.saveAll(raised);
        });
        boolean referenceLive = !products.isEmpty() && products.stream().allMatch(Product::isLiveData);
        log.info("Simulated {} listings for {} SKUs, raised {} alerts", generated.size(), products.size(), raised.size());
        return new RunResult(ApiEnvelope.SIMULATED, products.size(), generated.size(), raised.size(), referenceLive, now);
    }

    public ApiEnvelope<List<AlertDto>> alerts(Severity severity, AlertType type) {
        ensureRun();
        List<AlertDto> data = alerts.findAll().stream()
                .filter(a -> severity == null || a.getSeverity() == severity)
                .filter(a -> type == null || a.getType() == type)
                .sorted(Comparator.comparing(ReconciliationAlert::getSeverity)
                        .thenComparing(ReconciliationAlert::getType)
                        .thenComparing(ReconciliationAlert::getSku))
                .map(AlertDto::from)
                .toList();
        return ApiEnvelope.simulated(referencePriceIsLive(), data.size(), data);
    }

    public ApiEnvelope<List<ListingDto>> listings() {
        ensureRun();
        List<ListingDto> data = listings.findAllByOrderBySkuAscPlatformAsc().stream().map(ListingDto::from).toList();
        return ApiEnvelope.simulated(referencePriceIsLive(), data.size(), data);
    }

    public ApiEnvelope<SkuReconciliationDto> bySku(Long productId) {
        ensureRun();
        List<PlatformListing> rows = listings.findByProductIdOrderByPlatformAsc(productId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No reconciliation data for product " + productId);
        }
        PlatformListing first = rows.get(0);
        SkuReconciliationDto dto = new SkuReconciliationDto(first.getProductId(), first.getSku(), first.getProductName(),
                first.getFlavor(), first.getPackSize(),
                rows.stream().map(ListingDto::from).toList(),
                alerts.findByProductId(productId).stream().map(AlertDto::from).toList());
        return ApiEnvelope.simulated(referencePriceIsLive(), 1, dto);
    }

    /** Lazily simulates on first read so startup order between services does not matter. */
    private void ensureRun() {
        if (listings.count() == 0) {
            run();
        }
    }

    private boolean referencePriceIsLive() {
        List<PlatformListing> refs = listings.findAll().stream()
                .filter(l -> l.getPlatform() == Platform.BIGBASKET).toList();
        return !refs.isEmpty() && refs.stream().allMatch(l -> "BIGBASKET_SCRAPED".equals(l.getPriceSource()));
    }
}
