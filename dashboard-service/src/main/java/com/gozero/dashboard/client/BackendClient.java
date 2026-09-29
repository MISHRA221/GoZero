package com.gozero.dashboard.client;

import com.gozero.dashboard.client.BackendDtos.Alert;
import com.gozero.dashboard.client.BackendDtos.Complaint;
import com.gozero.dashboard.client.BackendDtos.Envelope;
import com.gozero.dashboard.client.BackendDtos.FlavorSentiment;
import com.gozero.dashboard.client.BackendDtos.Listing;
import com.gozero.dashboard.client.BackendDtos.Product;
import com.gozero.dashboard.config.ServiceProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class BackendClient {

    private static final ParameterizedTypeReference<Envelope<List<Product>>> PRODUCTS =
            new ParameterizedTypeReference<Envelope<List<Product>>>() { };
    private static final ParameterizedTypeReference<Envelope<List<FlavorSentiment>>> FLAVORS =
            new ParameterizedTypeReference<Envelope<List<FlavorSentiment>>>() { };
    private static final ParameterizedTypeReference<Envelope<List<Complaint>>> COMPLAINTS =
            new ParameterizedTypeReference<Envelope<List<Complaint>>>() { };
    private static final ParameterizedTypeReference<Envelope<List<Alert>>> ALERTS =
            new ParameterizedTypeReference<Envelope<List<Alert>>>() { };
    private static final ParameterizedTypeReference<Envelope<List<Listing>>> LISTINGS =
            new ParameterizedTypeReference<Envelope<List<Listing>>>() { };
    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<Map<String, Object>>() { };

    private final RestTemplate rest;
    private final ServiceProperties urls;

    public BackendClient(RestTemplate rest, ServiceProperties urls) {
        this.rest = rest;
        this.urls = urls;
    }

    public Envelope<List<Product>> products() {
        return get(urls.ingestionUrl() + "/api/products", PRODUCTS);
    }

    public Envelope<List<FlavorSentiment>> sentimentByFlavor() {
        return get(urls.sentimentUrl() + "/api/sentiment/by-flavor", FLAVORS);
    }

    public Envelope<List<Complaint>> complaints() {
        return get(urls.sentimentUrl() + "/api/sentiment/complaints", COMPLAINTS);
    }

    public Envelope<List<Alert>> alerts() {
        return get(urls.reconciliationUrl() + "/api/reconciliation/alerts", ALERTS);
    }

    public Envelope<List<Listing>> listings() {
        return get(urls.reconciliationUrl() + "/api/reconciliation/listings", LISTINGS);
    }

    public Map<String, Object> refreshIngestion() {
        return post(urls.ingestionUrl() + "/api/ingest/refresh");
    }

    public Map<String, Object> recomputeSentiment() {
        return post(urls.sentimentUrl() + "/api/sentiment/recompute");
    }

    public Map<String, Object> runReconciliation() {
        return post(urls.reconciliationUrl() + "/api/reconciliation/run");
    }

    private <T> T get(String url, ParameterizedTypeReference<T> type) {
        return rest.exchange(url, HttpMethod.GET, null, type).getBody();
    }

    private Map<String, Object> post(String url) {
        return rest.exchange(url, HttpMethod.POST, null, MAP).getBody();
    }
}
