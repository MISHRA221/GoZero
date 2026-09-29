package com.gozero.reconciliation.web;

import com.gozero.reconciliation.domain.ReconciliationAlert;

import java.time.Instant;

public record AlertDto(
        Long id,
        Long productId,
        String sku,
        String productName,
        String flavor,
        String type,
        String severity,
        String platforms,
        String message,
        Double metricValue,
        Instant createdAt,
        String dataSource) {

    public static AlertDto from(ReconciliationAlert a) {
        return new AlertDto(a.getId(), a.getProductId(), a.getSku(), a.getProductName(), a.getFlavor(),
                a.getType().name(), a.getSeverity().name(), a.getPlatforms(), a.getMessage(), a.getMetricValue(),
                a.getCreatedAt(), ApiEnvelope.SIMULATED);
    }
}
