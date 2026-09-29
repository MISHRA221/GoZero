package com.gozero.sentiment.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record RecomputeResult(
        int productsScanned,
        int reviewsAnalyzed,
        int negativeReviews,
        @JsonProperty("isLiveData") boolean isLiveData,
        Instant analyzedAt) {
}
