package com.gozero.reconciliation.service;

import java.time.Instant;

public record RunResult(
        String dataSource,
        int skus,
        int listings,
        int alerts,
        boolean referencePriceIsLive,
        Instant generatedAt) {
}
