package com.gozero.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reviews", indexes = @Index(name = "idx_reviews_product", columnList = "product_id"))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(length = 120)
    private String author;

    private Integer rating;

    @Column(name = "review_text", nullable = false, length = 2000)
    private String text;

    @Column(name = "is_live_data", nullable = false)
    private boolean liveData;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isLiveData() { return liveData; }
    public void setLiveData(boolean liveData) { this.liveData = liveData; }
    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
}
