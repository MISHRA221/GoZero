package com.gozero.sentiment.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gozero.sentiment.analysis.ComplaintCategory;
import com.gozero.sentiment.domain.ReviewSentiment;

public record ComplaintDto(
        Long reviewId,
        Long productId,
        String productName,
        String flavor,
        String category,
        String categoryLabel,
        Integer starRating,
        double score,
        String label,
        String snippet,
        String matchedTerms,
        @JsonProperty("isLiveData") boolean isLiveData) {

    private static final int SNIPPET_LENGTH = 220;

    public static ComplaintDto from(ReviewSentiment r) {
        String text = r.getReviewText() == null ? "" : r.getReviewText();
        String snippet = text.length() > SNIPPET_LENGTH ? text.substring(0, SNIPPET_LENGTH) + "..." : text;
        return new ComplaintDto(r.getReviewId(), r.getProductId(), r.getProductName(), r.getFlavor(), r.getCategory(),
                ComplaintCategory.valueOf(r.getCategory()).label(), r.getStarRating(), r.getNormalizedScore(),
                r.getLabel(), snippet, r.getMatchedTerms(), r.isLiveData());
    }
}
