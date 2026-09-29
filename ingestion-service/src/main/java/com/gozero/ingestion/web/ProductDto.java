package com.gozero.ingestion.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gozero.ingestion.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDto(
        Long id,
        String sku,
        String name,
        String flavor,
        String packSize,
        BigDecimal price,
        BigDecimal mrp,
        Double rating,
        Integer ratingCount,
        String productUrl,
        String source,
        @JsonProperty("isLiveData") boolean isLiveData,
        Instant ingestedAt) {

    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getFlavor(), p.getPackSize(), p.getPrice(),
                p.getMrp(), p.getRating(), p.getRatingCount(), p.getProductUrl(), p.getSource(), p.isLiveData(),
                p.getIngestedAt());
    }
}
