package com.gozero.ingestion.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record RefreshResult(
        @JsonProperty("isLiveData") boolean isLiveData,
        String dataSource,
        int productCount,
        int reviewCount,
        int liveReviewCount,
        String fallbackReason,
        Instant refreshedAt) {
}
