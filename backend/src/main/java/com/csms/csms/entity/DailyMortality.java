package com.csms.csms.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "daily_mortality")
@DynamicInsert
public class DailyMortality {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mortality_id")
    private UUID mortalityId;

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "count", nullable = false)
    private Integer count;

    // 'DAY' or 'NIGHT'  — VARCHAR(10) in DB, validated by CHECK constraint
    @Column(name = "shift", nullable = false, length = 10)
    private String shift;

    @Column(name = "type")
    private String type;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public DailyMortality() {}

    public DailyMortality(UUID flockId, LocalDate recordDate,
                          Integer count, String shift, String type, UUID recordedBy) {
        this.flockId    = flockId;
        this.recordDate = recordDate;
        this.count      = count;
        this.shift      = shift;
        this.type       = type;
        this.recordedBy = recordedBy;
    }

    public UUID getMortalityId()                     { return mortalityId; }
    public void setMortalityId(UUID v)               { this.mortalityId = v; }

    public UUID getFlockId()                         { return flockId; }
    public void setFlockId(UUID v)                   { this.flockId = v; }

    public LocalDate getRecordDate()                 { return recordDate; }
    public void setRecordDate(LocalDate v)           { this.recordDate = v; }

    public Integer getCount()                        { return count; }
    public void setCount(Integer v)                  { this.count = v; }

    public String getShift()                         { return shift; }
    public void setShift(String v)                   { this.shift = v; }

    public String getType()                          { return type; }
    public void setType(String v)                    { this.type = v; }

    public UUID getRecordedBy()                      { return recordedBy; }
    public void setRecordedBy(UUID v)                { this.recordedBy = v; }

    public OffsetDateTime getCreatedAt()             { return createdAt; }
}
