package com.csms.csms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "feed_purchases")
public class FeedPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "purchase_id")
    private UUID purchaseId;

    @Column(name = "feed_type_id", nullable = false)
    private UUID feedTypeId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "sack_count", nullable = false)
    private Integer sackCount;

    @Column(name = "cost_per_sack", nullable = false, precision = 10, scale = 2)
    private BigDecimal costPerSack;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public FeedPurchase() {}

    public FeedPurchase(UUID feedTypeId, UUID supplierId, LocalDate purchaseDate, 
                       Integer sackCount, BigDecimal costPerSack) {
        this.feedTypeId = feedTypeId;
        this.supplierId = supplierId;
        this.purchaseDate = purchaseDate;
        this.sackCount = sackCount;
        this.costPerSack = costPerSack;
        this.totalCost = costPerSack.multiply(new BigDecimal(sackCount));
    }

    // Getters & Setters
    public UUID getPurchaseId() { return purchaseId; }
    public void setPurchaseId(UUID purchaseId) { this.purchaseId = purchaseId; }

    public UUID getFeedTypeId() { return feedTypeId; }
    public void setFeedTypeId(UUID feedTypeId) { this.feedTypeId = feedTypeId; }

    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public Integer getSackCount() { return sackCount; }
    public void setSackCount(Integer sackCount) { this.sackCount = sackCount; }

    public BigDecimal getCostPerSack() { return costPerSack; }
    public void setCostPerSack(BigDecimal costPerSack) { this.costPerSack = costPerSack; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}