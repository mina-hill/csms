package com.csms.csms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "weekly_weight")
public class WeeklyWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "weight_id")
    private UUID weightId;

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    // NUMERIC(6,3)
    @Column(name = "avg_weight_kg", nullable = false, precision = 6, scale = 3)
    private BigDecimal avgWeightKg;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public WeeklyWeight() {}

    public WeeklyWeight(UUID flockId, Integer weekNumber, LocalDate recordDate,
                        BigDecimal avgWeightKg, UUID recordedBy) {
        this.flockId      = flockId;
        this.weekNumber   = weekNumber;
        this.recordDate   = recordDate;
        this.avgWeightKg  = avgWeightKg;
        this.recordedBy   = recordedBy;
    }

    public UUID getWeightId()                        { return weightId; }
    public void setWeightId(UUID v)                  { this.weightId = v; }

    public UUID getFlockId()                         { return flockId; }
    public void setFlockId(UUID v)                   { this.flockId = v; }

    public Integer getWeekNumber()                   { return weekNumber; }
    public void setWeekNumber(Integer v)             { this.weekNumber = v; }

    public LocalDate getRecordDate()                 { return recordDate; }
    public void setRecordDate(LocalDate v)           { this.recordDate = v; }

    public BigDecimal getAvgWeightKg()               { return avgWeightKg; }
    public void setAvgWeightKg(BigDecimal v)         { this.avgWeightKg = v; }

    public UUID getRecordedBy()                      { return recordedBy; }
    public void setRecordedBy(UUID v)                { this.recordedBy = v; }

    public OffsetDateTime getCreatedAt()             { return createdAt; }
}
