package com.gozero.dashboard.service;

import com.gozero.dashboard.client.BackendClient;
import com.gozero.dashboard.client.BackendDtos.Alert;
import com.gozero.dashboard.client.BackendDtos.Complaint;
import com.gozero.dashboard.client.BackendDtos.Envelope;
import com.gozero.dashboard.client.BackendDtos.FlavorSentiment;
import com.gozero.dashboard.client.BackendDtos.Listing;
import com.gozero.dashboard.client.BackendDtos.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private BackendClient backendClient;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        backendClient = mock(BackendClient.class);
        dashboardService = new DashboardService(backendClient);
    }

    @Test
    void buildAggregatesDashboardKpisFromBackendResponses() {
        Product product = new Product(1L, "SKU-1", "Go Zero Vanilla", "Vanilla", "500 ml",
                new BigDecimal("250.00"), new BigDecimal("300.00"), 4.2, 12,
                "BIGBASKET", true);
        FlavorSentiment flavor = new FlavorSentiment("Vanilla", 10, 6, 2, 2, 20.0,
                0.4, "Taste", true);
        Alert alert = new Alert(1L, 1L, "SKU-1", "Go Zero Vanilla", "Vanilla",
                "PRICE_VARIANCE", "HIGH", "BLINKIT", "Price differs", 15.0,
                "SIMULATED");

        when(backendClient.products()).thenReturn(envelope(true, "BIGBASKET_SCRAPED", List.of(product)));
        when(backendClient.sentimentByFlavor()).thenReturn(envelope(true, "LIVE_REVIEWS", List.of(flavor)));
        when(backendClient.complaints()).thenReturn(envelope(true, "LIVE_REVIEWS", List.<Complaint>of()));
        when(backendClient.alerts()).thenReturn(envelope(false, "SIMULATED", List.of(alert)));
        when(backendClient.listings()).thenReturn(envelope(false, "SIMULATED", List.<Listing>of()));

        DashboardView view = dashboardService.build();

        assertThat(view.kpis().skusTracked()).isEqualTo(1);
        assertThat(view.kpis().avgRating()).isEqualTo(4.2);
        assertThat(view.kpis().negativePct()).isEqualTo(20.0);
        assertThat(view.kpis().reviewsAnalyzed()).isEqualTo(10);
        assertThat(view.kpis().activeAlerts()).isEqualTo(1);
        assertThat(view.kpis().highAlerts()).isEqualTo(1);
        assertThat(view.services()).hasSize(3).allSatisfy(status -> assertThat(status.up()).isTrue());
    }

    @Test
    void buildReturnsSafeEmptyDashboardWhenBackendsAreUnavailable() {
        when(backendClient.products()).thenThrow(new RestClientException("ingestion unavailable"));
        when(backendClient.sentimentByFlavor()).thenThrow(new RestClientException("sentiment unavailable"));
        when(backendClient.complaints()).thenThrow(new RestClientException("sentiment unavailable"));
        when(backendClient.alerts()).thenThrow(new RestClientException("reconciliation unavailable"));
        when(backendClient.listings()).thenThrow(new RestClientException("reconciliation unavailable"));

        DashboardView view = dashboardService.build();

        assertThat(view.kpis().skusTracked()).isZero();
        assertThat(view.kpis().avgRating()).isZero();
        assertThat(view.kpis().reviewsAnalyzed()).isZero();
        assertThat(view.alerts()).isEmpty();
        assertThat(view.provenance().productsSource()).isEqualTo("UNAVAILABLE");
        assertThat(view.services()).hasSize(3).allSatisfy(status -> assertThat(status.up()).isFalse());
    }

    @Test
    void refreshPipelineContinuesAfterOneBackendStepFails() {
        when(backendClient.refreshIngestion()).thenThrow(new RestClientException("scrape failed"));
        when(backendClient.recomputeSentiment()).thenReturn(Map.of("status", "ok"));
        when(backendClient.runReconciliation()).thenReturn(Map.of("status", "ok"));

        List<String> results = dashboardService.refreshPipeline();

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).contains("Ingestion: FAILED - scrape failed");
        assertThat(results.get(1)).contains("Sentiment:");
        assertThat(results.get(2)).contains("Reconciliation:");
        var order = inOrder(backendClient);
        order.verify(backendClient).refreshIngestion();
        order.verify(backendClient).recomputeSentiment();
        order.verify(backendClient).runReconciliation();
    }

    private static <T> Envelope<List<T>> envelope(boolean live, String source, List<T> data) {
        return new Envelope<>(live, source, "test data", null, Instant.now(), data.size(), data);
    }
}
