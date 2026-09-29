package com.gozero.reconciliation.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

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
                          BigDecimal mrp, @JsonProperty("isLiveData") boolean isLiveData) {
    }
}
