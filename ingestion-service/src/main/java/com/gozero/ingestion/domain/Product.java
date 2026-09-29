package com.gozero.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 120)
    private String flavor;

    @Column(name = "pack_size", length = 60)
    private String packSize;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal mrp;

    private Double rating;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Column(name = "product_url", length = 500)
    private String productUrl;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "is_live_data", nullable = false)
    private boolean liveData;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFlavor() { return flavor; }
    public void setFlavor(String flavor) { this.flavor = flavor; }
    public String getPackSize() { return packSize; }
    public void setPackSize(String packSize) { this.packSize = packSize; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isLiveData() { return liveData; }
    public void setLiveData(boolean liveData) { this.liveData = liveData; }
    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
}
