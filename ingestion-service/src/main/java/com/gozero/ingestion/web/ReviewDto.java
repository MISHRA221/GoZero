package com.gozero.ingestion.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gozero.ingestion.domain.Review;

public record ReviewDto(
        Long id,
        Long productId,
        String author,
        Integer rating,
        String text,
        @JsonProperty("isLiveData") boolean isLiveData) {

    public static ReviewDto from(Review r) {
        return new ReviewDto(r.getId(), r.getProductId(), r.getAuthor(), r.getRating(), r.getText(), r.isLiveData());
    }
}
