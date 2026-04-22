package com.csms.csms.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "medicine_purchases")
@DynamicInsert
public class MedicinePurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "purchase_id")
    private UUID purchaseId;

    @Column(name = "medicine_id", nullable = false)
    private UUID medicineId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    /**
     * Matches DB generated column {@code total_cost}; mapped as a formula so Hibernate 6 does not
     * emit it in INSERT (insertable=false is not always honored for generated columns).
     */
    @Formula("quantity * unit_cost")
    private BigDecimal totalCost;

    @Column(name = "unit", length = 60)
    private String unit;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    /** Same as feed/brada purchases: DB default (e.g. {@code now()}). */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public MedicinePurchase() {}

    public MedicinePurchase(UUID medicineId, UUID supplierId, LocalDate purchaseDate, 
                           Integer quantity, BigDecimal unitCost) {
        this.medicineId = medicineId;
        this.supplierId = supplierId;
        this.purchaseDate = purchaseDate;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    // Getters & Setters
    public UUID getPurchaseId() { return purchaseId; }
    public void setPurchaseId(UUID purchaseId) { this.purchaseId = purchaseId; }

    public UUID getMedicineId() { return medicineId; }
    public void setMedicineId(UUID medicineId) { this.medicineId = medicineId; }

    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}