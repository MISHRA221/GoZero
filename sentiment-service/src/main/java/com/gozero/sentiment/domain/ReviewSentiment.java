package com.gozero.sentiment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "review_sentiment", indexes = {
        @Index(name = "idx_sentiment_flavor", columnList = "flavor"),
        @Index(name = "idx_sentiment_category", columnList = "category")
})
public class ReviewSentiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 300)
    private String productName;

    @Column(length = 120)
    private String flavor;

    @Column(name = "review_text", length = 2000)
    private String reviewText;

    @Column(name = "star_rating")
    private Integer starRating;

    @Column(name = "raw_score", nullable = false)
    private double rawScore;

    @Column(name = "normalized_score", nullable = false)
    private double normalizedScore;

    @Column(nullable = false, length = 16)
    private String label;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(name = "matched_terms", length = 1000)
    private String matchedTerms;

    @Column(name = "is_live_data", nullable = false)
    private boolean liveData;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    public Long getId() { return id; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getFlavor() { return flavor; }
    public void setFlavor(String flavor) { this.flavor = flavor; }
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    public Integer getStarRating() { return starRating; }
    public void setStarRating(Integer starRating) { this.starRating = starRating; }
    public double getRawScore() { return rawScore; }
    public void setRawScore(double rawScore) { this.rawScore = rawScore; }
    public double getNormalizedScore() { return normalizedScore; }
    public void setNormalizedScore(double normalizedScore) { this.normalizedScore = normalizedScore; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getMatchedTerms() { return matchedTerms; }
    public void setMatchedTerms(String matchedTerms) { this.matchedTerms = matchedTerms; }
    public boolean isLiveData() { return liveData; }
    public void setLiveData(boolean liveData) { this.liveData = liveData; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(Instant analyzedAt) { this.analyzedAt = analyzedAt; }
}
