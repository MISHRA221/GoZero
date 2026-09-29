package com.gozero.dashboard.service;

import com.gozero.dashboard.client.BackendClient;
import com.gozero.dashboard.client.BackendDtos.Alert;
import com.gozero.dashboard.client.BackendDtos.Complaint;
import com.gozero.dashboard.client.BackendDtos.Envelope;
import com.gozero.dashboard.client.BackendDtos.FlavorSentiment;
import com.gozero.dashboard.client.BackendDtos.Listing;
import com.gozero.dashboard.client.BackendDtos.Product;
import com.gozero.dashboard.service.DashboardView.ChartSeries;
import com.gozero.dashboard.service.DashboardView.FlavorRow;
import com.gozero.dashboard.service.DashboardView.Kpis;
import com.gozero.dashboard.service.DashboardView.PriceChart;
import com.gozero.dashboard.service.DashboardView.PriceSeries;
import com.gozero.dashboard.service.DashboardView.Provenance;
import com.gozero.dashboard.service.DashboardView.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final List<String> PLATFORM_ORDER = List.of("BIGBASKET", "BLINKIT", "ZEPTO", "INSTAMART");
    private static final int MAX_PRICE_CHART_SKUS = 12;
    private static final int MAX_SNIPPETS_PER_FLAVOR = 5;

    private final BackendClient client;

    public DashboardService(BackendClient client) {
        this.client = client;
    }

    public DashboardView build() {
        Map<String, ServiceStatus> statuses = new LinkedHashMap<>();
        Envelope<List<Product>> products = call("ingestion-service", client::products, statuses);
        Envelope<List<FlavorSentiment>> sentiment = call("sentiment-service", client::sentimentByFlavor, statuses);
        Envelope<List<Complaint>> complaints = call("sentiment-service", client::complaints, statuses);
        Envelope<List<Alert>> alerts = call("reconciliation-service", client::alerts, statuses);
        Envelope<List<Listing>> listings = call("reconciliation-service", client::listings, statuses);

        List<Product> productList = data(products);
        List<FlavorSentiment> flavorList = data(sentiment);
        List<Complaint> complaintList = data(complaints);
        List<Alert> alertList = data(alerts);
        List<Listing> listingList = data(listings);

        boolean productsLive = products != null && Boolean.TRUE.equals(products.isLiveData());
        boolean reviewsLive = sentiment != null && Boolean.TRUE.equals(sentiment.isLiveData());
        Provenance provenance = new Provenance(
                productsLive,
                products == null ? "UNAVAILABLE" : products.dataSource(),
                products == null ? "ingestion-service unreachable" : products.note(),
                reviewsLive,
                sentiment == null ? "UNAVAILABLE" : sentiment.dataSource(),
                alerts == null ? "UNAVAILABLE" : alerts.dataSource(),
                alerts != null && Boolean.TRUE.equals(alerts.referencePriceIsLive()),
                alerts == null ? "reconciliation-service unreachable" : alerts.note());

        int reviewCount = flavorList.stream().mapToInt(FlavorSentiment::reviewCount).sum();
        int negativeCount = flavorList.stream().mapToInt(FlavorSentiment::negative).sum();
        double avgRating = productList.stream().map(Product::rating).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average().orElse(0);
        long high = alertList.stream().filter(a -> "HIGH".equals(a.severity())).count();
        Kpis kpis = new Kpis(productList.size(), round(avgRating, 2),
                reviewCount == 0 ? 0 : round(negativeCount * 100.0 / reviewCount, 1), reviewCount, alertList.size(), high);

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (String s : List.of("HIGH", "MEDIUM", "LOW")) {
            bySeverity.put(s, alertList.stream().filter(a -> s.equals(a.severity())).count());
        }

        return new DashboardView(Instant.now(), provenance, kpis, ratingByFlavor(productList),
                flavorRows(flavorList, complaintList), complaintCategories(complaintList), alertList, bySeverity,
                priceChart(listingList, alertList), new ArrayList<>(statuses.values()));
    }

    /** Runs the whole pipeline in dependency order: scrape -> sentiment -> reconciliation. */
    public List<String> refreshPipeline() {
        List<String> steps = new ArrayList<>();
        steps.add(step("Ingestion", client::refreshIngestion));
        steps.add(step("Sentiment", client::recomputeSentiment));
        steps.add(step("Reconciliation", client::runReconciliation));
        return steps;
    }

    private static String step(String name, Supplier<Map<String, Object>> action) {
        try {
            return name + ": " + action.get();
        } catch (RestClientException e) {
            return name + ": FAILED - " + e.getMessage();
        }
    }

    private static ChartSeries ratingByFlavor(List<Product> products) {
        Map<String, Double> avg = products.stream()
                .filter(p -> p.rating() != null)
                .collect(Collectors.groupingBy(p -> Objects.requireNonNullElse(p.flavor(), "Unknown"),
                        Collectors.averagingDouble(Product::rating)));
        List<Map.Entry<String, Double>> sorted = avg.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();
        return new ChartSeries(sorted.stream().map(Map.Entry::getKey).toList(),
                sorted.stream().map(e -> (Number) round(e.getValue(), 2)).toList());
    }

    private static List<FlavorRow> flavorRows(List<FlavorSentiment> flavors, List<Complaint> complaints) {
        Map<String, List<Complaint>> byFlavor = complaints.stream()
                .collect(Collectors.groupingBy(c -> Objects.requireNonNullElse(c.flavor(), "Unknown")));
        return flavors.stream()
                .map(f -> new FlavorRow(f.flavor(), f.reviewCount(), f.positive(), f.neutral(), f.negative(),
                        f.negativePct(), f.topComplaintCategory(),
                        byFlavor.getOrDefault(f.flavor(), List.of()).stream().limit(MAX_SNIPPETS_PER_FLAVOR).toList()))
                .toList();
    }

    private static ChartSeries complaintCategories(List<Complaint> complaints) {
        Map<String, Long> counts = complaints.stream()
                .collect(Collectors.groupingBy(Complaint::categoryLabel, Collectors.counting()));
        List<Map.Entry<String, Long>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();
        return new ChartSeries(sorted.stream().map(Map.Entry::getKey).toList(),
                sorted.stream().map(e -> (Number) e.getValue()).toList());
    }

    /** SKUs with price alerts first, topped up with other SKUs, so the chart stays readable. */
    private static PriceChart priceChart(List<Listing> listings, List<Alert> alerts) {
        Map<Long, List<Listing>> byProduct = listings.stream()
                .collect(Collectors.groupingBy(Listing::productId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> chosen = new LinkedHashSet<>();
        alerts.stream().filter(a -> "PRICE_VARIANCE".equals(a.type())).map(Alert::productId)
                .filter(byProduct::containsKey).forEach(chosen::add);
        for (Long id : byProduct.keySet()) {
            if (chosen.size() >= MAX_PRICE_CHART_SKUS) {
                break;
            }
            chosen.add(id);
        }
        List<Long> ids = chosen.stream().limit(MAX_PRICE_CHART_SKUS).toList();

        List<String> labels = ids.stream().map(id -> {
            Listing any = byProduct.get(id).get(0);
            return any.flavor() + " " + Objects.requireNonNullElse(any.packSize(), "");
        }).toList();

        List<PriceSeries> series = new ArrayList<>();
        for (String platform : PLATFORM_ORDER) {
            String name = listings.stream().filter(l -> platform.equals(l.platform())).map(Listing::platformName)
                    .findFirst().orElse(platform);
            String source = listings.stream().filter(l -> platform.equals(l.platform())).map(Listing::priceSource)
                    .findFirst().orElse("SIMULATED");
            String suffix = switch (source) {
                case "BIGBASKET_SCRAPED" -> " (scraped)";
                case "SEED_DATASET" -> " (seed data)";
                default -> " (SIMULATED)";
            };
            List<BigDecimal> prices = ids.stream().map(id -> byProduct.get(id).stream()
                    .filter(l -> platform.equals(l.platform()) && l.listed())
                    .map(Listing::price).findFirst().orElse(null)).toList();
            series.add(new PriceSeries(platform, name + suffix, prices));
        }
        return new PriceChart(labels, series);
    }

    private static <T> T call(String service, Supplier<T> supplier, Map<String, ServiceStatus> statuses) {
        try {
            T result = supplier.get();
            statuses.putIfAbsent(service, new ServiceStatus(service, true, "OK"));
            return result;
        } catch (RestClientException e) {
            log.warn("{} call failed: {}", service, e.getMessage());
            statuses.put(service, new ServiceStatus(service, false, e.getMessage()));
            return null;
        }
    }

    private static <T> List<T> data(Envelope<List<T>> env) {
        return env == null || env.data() == null ? List.of() : env.data();
    }

    private static double round(double v, int places) {
        double f = Math.pow(10, places);
        return Math.round(v * f) / f;
    }
}
