package com.gozero.dashboard.service;

import com.gozero.dashboard.client.BackendDtos.Alert;
import com.gozero.dashboard.client.BackendDtos.Complaint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Everything the dashboard page (and GET /api/dashboard/summary) needs, pre-aggregated. */
public record DashboardView(
        Instant generatedAt,
        Provenance provenance,
        Kpis kpis,
        ChartSeries ratingByFlavor,
        List<FlavorRow> flavorRows,
        ChartSeries complaintCategories,
        List<Alert> alerts,
        Map<String, Long> alertsBySeverity,
        PriceChart priceChart,
        List<ServiceStatus> services) {

    public record Provenance(boolean productsLive, String productsSource, String productsNote,
                             boolean reviewsLive, String reviewsSource,
                             String reconciliationSource, boolean referencePriceIsLive, String reconciliationNote) {
    }

    public record Kpis(int skusTracked, double avgRating, double negativePct, int reviewsAnalyzed,
                       int activeAlerts, long highAlerts) {
    }

    public record ChartSeries(List<String> labels, List<Number> values) {
    }

    public record FlavorRow(String flavor, int reviewCount, int positive, int neutral, int negative,
                            double negativePct, String topComplaint, List<Complaint> complaints) {
    }

    public record PriceChart(List<String> labels, List<PriceSeries> series) {
    }

    public record PriceSeries(String platform, String label, List<BigDecimal> prices) {
    }

    public record ServiceStatus(String name, boolean up, String detail) {
    }
}
