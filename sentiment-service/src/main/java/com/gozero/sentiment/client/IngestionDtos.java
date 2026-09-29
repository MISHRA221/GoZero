package com.gozero.sentiment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Read-only views of ingestion-service responses (only the fields this service needs). */
public final class IngestionDtos {

    private IngestionDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope<T>(@JsonProperty("isLiveData") boolean isLiveData, String dataSource, String note,
                              Instant generatedAt, int count, T data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(Long id, String sku, String name, String flavor, String packSize, BigDecimal price,
                          BigDecimal mrp, Double rating, Integer ratingCount,
                          @JsonProperty("isLiveData") boolean isLiveData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Review(Long id, Long productId, String author, Integer rating, String text,
                         @JsonProperty("isLiveData") boolean isLiveData) {
    }

    public static <T> List<T> dataOrEmpty(Envelope<List<T>> env) {
        return env == null || env.data() == null ? List.of() : env.data();
    }
}
