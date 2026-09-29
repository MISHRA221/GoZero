package com.gozero.reconciliation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alerts")
public class ReconciliationAlert {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 24)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Severity severity;

    @Column(length = 120)
    private String platforms;

    @Column(length = 500)
    private String message;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getFlavor() { return flavor; }
    public void setFlavor(String flavor) { this.flavor = flavor; }
    public AlertType getType() { return type; }
    public void setType(AlertType type) { this.type = type; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getPlatforms() { return platforms; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Double getMetricValue() { return metricValue; }
    public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
