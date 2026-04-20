package com.csms.csms.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "medicines")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "medicine_id")
    private UUID medicineId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "min_threshold", nullable = false)
    private Integer minThreshold = 5;

    @Column(name = "current_stock", nullable = false)
    private Integer currentStock = 0;

    @Column(name = "last_updated", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime lastUpdated;

    // Constructors
    public Medicine() {}

    public Medicine(String name) {
        this.name = name;
        this.minThreshold = 5;
        this.currentStock = 0;
    }

    public Medicine(String name, Integer minThreshold) {
        this.name = name;
        this.minThreshold = minThreshold;
        this.currentStock = 0;
    }

    // Getters & Setters
    public UUID getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(UUID medicineId) {
        this.medicineId = medicineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(Integer minThreshold) {
        this.minThreshold = minThreshold;
    }

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Medicine{" +
                "medicineId=" + medicineId +
                ", name='" + name + '\'' +
                ", currentStock=" + currentStock +
                ", minThreshold=" + minThreshold +
                '}';
    }
}