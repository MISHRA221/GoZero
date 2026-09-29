package com.gozero.reconciliation.client;

import com.gozero.reconciliation.client.IngestionDtos.Envelope;
import com.gozero.reconciliation.client.IngestionDtos.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class IngestionClient {

    private static final ParameterizedTypeReference<Envelope<List<Product>>> PRODUCTS =
            new ParameterizedTypeReference<Envelope<List<Product>>>() { };

    private final RestTemplate rest;
    private final String baseUrl;

    public IngestionClient(RestTemplate rest, @Value("${gozero.services.ingestion-url}") String baseUrl) {
        this.rest = rest;
        this.baseUrl = baseUrl;
    }

    public List<Product> products() {
        Envelope<List<Product>> env = rest.exchange(baseUrl + "/api/products", HttpMethod.GET, null, PRODUCTS).getBody();
        return env == null || env.data() == null ? List.of() : env.data();
    }
}
