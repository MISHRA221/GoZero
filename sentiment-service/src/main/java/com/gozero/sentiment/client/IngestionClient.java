package com.gozero.sentiment.client;

import com.gozero.sentiment.client.IngestionDtos.Envelope;
import com.gozero.sentiment.client.IngestionDtos.Product;
import com.gozero.sentiment.client.IngestionDtos.Review;
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
    private static final ParameterizedTypeReference<Envelope<List<Review>>> REVIEWS =
            new ParameterizedTypeReference<Envelope<List<Review>>>() { };

    private final RestTemplate rest;
    private final String baseUrl;

    public IngestionClient(RestTemplate rest, @Value("${gozero.services.ingestion-url}") String baseUrl) {
        this.rest = rest;
        this.baseUrl = baseUrl;
    }

    public List<Product> products() {
        return IngestionDtos.dataOrEmpty(
                rest.exchange(baseUrl + "/api/products", HttpMethod.GET, null, PRODUCTS).getBody());
    }

    public List<Review> reviews(long productId) {
        return IngestionDtos.dataOrEmpty(
                rest.exchange(baseUrl + "/api/products/{id}/reviews", HttpMethod.GET, null, REVIEWS, productId).getBody());
    }
}
