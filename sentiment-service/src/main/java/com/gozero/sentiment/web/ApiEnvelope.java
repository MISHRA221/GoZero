package com.gozero.sentiment.web;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ApiEnvelope<T>(
        @JsonProperty("isLiveData") boolean isLiveData,
        String dataSource,
        String note,
        Instant generatedAt,
        int count,
        T data) {
}
