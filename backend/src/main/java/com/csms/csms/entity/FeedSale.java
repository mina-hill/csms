package com.csms.csms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Formula;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sacks_sale")
@DynamicInsert
public class FeedSale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "sacks_sold", nullable = false)
    private Integer sacksSold;

    @Column(name = "price_per_sack", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSack;

    @Formula("sacks_sold * price_per_sack")
    private BigDecimal totalRevenue;

    @Column(name = "buyer_name", nullable = false, length = 100)
    private String buyerName;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public FeedSale() {}

    public FeedSale(LocalDate saleDate, Integer sacksSold, BigDecimal pricePerSack, String buyerName) {
        this.saleDate = saleDate;
        this.sacksSold = sacksSold;
        this.pricePerSack = pricePerSack;
        this.buyerName = buyerName;
    }

    // Getters & Setters
    public UUID getSaleId() { return saleId; }
    public void setSaleId(UUID saleId) { this.saleId = saleId; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public Integer getSacksSold() { return sacksSold; }
    public void setSacksSold(Integer sacksSold) { this.sacksSold = sacksSold; }

    public BigDecimal getPricePerSack() { return pricePerSack; }
    public void setPricePerSack(BigDecimal pricePerSack) { this.pricePerSack = pricePerSack; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}