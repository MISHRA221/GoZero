package com.gozero.dashboard.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/** Consumer-side views of the three backend services' responses. */
public final class BackendDtos {

    private BackendDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope<T>(@JsonProperty("isLiveData") Boolean isLiveData, String dataSource, String note,
                              Boolean referencePriceIsLive, Instant generatedAt, Integer count, T data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(Long id, String sku, String name, String flavor, String packSize, BigDecimal price,
                          BigDecimal mrp, Double rating, Integer ratingCount, String source,
                          @JsonProperty("isLiveData") boolean isLiveData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlavorSentiment(String flavor, int reviewCount, int positive, int neutral, int negative,
                                  double negativePct, double avgScore, String topComplaintCategory,
                                  @JsonProperty("isLiveData") boolean isLiveData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Complaint(Long reviewId, Long productId, String productName, String flavor, String category,
                            String categoryLabel, Integer starRating, double score, String snippet,
                            String matchedTerms, @JsonProperty("isLiveData") boolean isLiveData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alert(Long id, Long productId, String sku, String productName, String flavor, String type,
                        String severity, String platforms, String message, Double metricValue, String dataSource) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Listing(Long productId, String sku, String productName, String flavor, String packSize,
                          String platform, String platformName, BigDecimal price, BigDecimal mrp, boolean inStock,
                          boolean listed, Double deviationPct, String priceSource) {
    }
}
