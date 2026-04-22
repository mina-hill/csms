package com.csms.csms.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "medicine_usage")
@DynamicInsert
public class MedicineUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usage_id")
    private UUID usageId;

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "medicine_id", nullable = false)
    private UUID medicineId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "dosage", nullable = false, precision = 10, scale = 3)
    private BigDecimal dosage;

    @Column(name = "unit", length = 60)
    private String unit;

    @Column(name = "usage_time")
    private String usageTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public MedicineUsage() {}

    public MedicineUsage(UUID flockId, UUID medicineId, LocalDate usageDate, BigDecimal dosage) {
        this.flockId = flockId;
        this.medicineId = medicineId;
        this.usageDate = usageDate;
        this.dosage = dosage;
    }

    // Getters & Setters
    public UUID getUsageId() { return usageId; }
    public void setUsageId(UUID usageId) { this.usageId = usageId; }

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }

    public UUID getMedicineId() { return medicineId; }
    public void setMedicineId(UUID medicineId) { this.medicineId = medicineId; }

    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }

    public BigDecimal getDosage() { return dosage; }
    public void setDosage(BigDecimal dosage) { this.dosage = dosage; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getUsageTime() { return usageTime; }
    public void setUsageTime(String usageTime) { this.usageTime = usageTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}