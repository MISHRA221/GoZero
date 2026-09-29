package com.gozero.reconciliation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "platform_listings", indexes = @Index(name = "idx_listing_product", columnList = "product_id"))
public class PlatformListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(length = 64)
    private String sku;

    @Column(name = "product_name", length = 300)
    private String productName;

    @Column(length = 120)
    private String flavor;

    @Column(name = "pack_size", length = 60)
    private String packSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Platform platform;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock;

    @Column(nullable = false)
    private boolean listed;

    @Column(name = "deviation_pct")
    private Double deviationPct;

    /** BIGBASKET_SCRAPED / SEED_DATASET for the reference row, SIMULATED for every other platform. */
    @Column(name = "price_source", nullable = false, length = 32)
    private String priceSource;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getFlavor() { return flavor; }
    public void setFlavor(String flavor) { this.flavor = flavor; }
    public String getPackSize() { return packSize; }
    public void setPackSize(String packSize) { this.packSize = packSize; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
    public boolean isListed() { return listed; }
    public void setListed(boolean listed) { this.listed = listed; }
    public Double getDeviationPct() { return deviationPct; }
    public void setDeviationPct(Double deviationPct) { this.deviationPct = deviationPct; }
    public String getPriceSource() { return priceSource; }
    public void setPriceSource(String priceSource) { this.priceSource = priceSource; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
