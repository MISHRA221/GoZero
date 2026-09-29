package com.gozero.reconciliation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gozero.reconciliation")
public record ReconciliationProperties(
        long randomSeed,
        double normalJitterPct,
        double anomalyMinPct,
        double anomalyMaxPct,
        double priceAnomalyRate,
        double stockAnomalyRate,
        double assortmentGapRate,
        double priceVarianceThresholdPct,
        double highSeverityPct) {
}
