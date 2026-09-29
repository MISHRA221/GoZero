package com.gozero.ingestion.scraper;

import java.math.BigDecimal;
import java.util.List;

public record ScrapedProduct(
        String externalId,
        String name,
        String flavor,
        String packSize,
        BigDecimal price,
        BigDecimal mrp,
        Double rating,
        Integer ratingCount,
        String url,
        List<ScrapedReview> reviews) {
}
