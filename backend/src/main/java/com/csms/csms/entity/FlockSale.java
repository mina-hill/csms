package com.csms.csms.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "flock_sales")
@DynamicInsert
public class FlockSale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "buyer_name", nullable = false, length = 100)
    private String buyerName;

    @Column(name = "qty_sold", nullable = false)
    private Integer qtySold;

    /** UI sends total sale weight (kg) for qty=1 lots; values often exceed NUMERIC(5,3). */
    @Column(name = "weight_per_bird_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal weightPerBirdKg;

    @Column(name = "price_per_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKg;

    /** Populated by the DB (generated column or default); never written by Hibernate. */
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2, insertable = false, updatable = false)
    @Generated(GenerationTime.INSERT)
    private BigDecimal totalAmount;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public FlockSale() {}

    public FlockSale(UUID flockId, LocalDate saleDate, String buyerName,
                     Integer qtySold, BigDecimal weightPerBirdKg, BigDecimal pricePerKg) {
        this.flockId = flockId;
        this.saleDate = saleDate;
        this.buyerName = buyerName;
        this.qtySold = qtySold;
        this.weightPerBirdKg = weightPerBirdKg;
        this.pricePerKg = pricePerKg;
    }

    // Getters & Setters
    public UUID getSaleId() { return saleId; }
    public void setSaleId(UUID saleId) { this.saleId = saleId; }

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public Integer getQtySold() { return qtySold; }
    public void setQtySold(Integer qtySold) { this.qtySold = qtySold; }

    public BigDecimal getWeightPerBirdKg() { return weightPerBirdKg; }
    public void setWeightPerBirdKg(BigDecimal weightPerBirdKg) { this.weightPerBirdKg = weightPerBirdKg; }

    public BigDecimal getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(BigDecimal pricePerKg) { this.pricePerKg = pricePerKg; }

    public BigDecimal getTotalAmount() { return totalAmount; }

    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
