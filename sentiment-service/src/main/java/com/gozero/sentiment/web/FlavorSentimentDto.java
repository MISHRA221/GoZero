package com.gozero.sentiment.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FlavorSentimentDto(
        String flavor,
        int reviewCount,
        int positive,
        int neutral,
        int negative,
        double negativePct,
        double avgScore,
        String topComplaintCategory,
        @JsonProperty("isLiveData") boolean isLiveData) {
}
