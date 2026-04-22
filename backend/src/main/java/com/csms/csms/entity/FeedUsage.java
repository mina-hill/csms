package com.csms.csms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "feed_usage", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"flock_id", "feed_type_id", "usage_date", "shift"}))
public class FeedUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usage_id")
    private UUID usageId;

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "feed_type_id", nullable = false)
    private UUID feedTypeId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "sacks_used", nullable = false)
    private Integer sacksUsed;

    @Column(name = "shift", nullable = false, length = 10)
    private String shift;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public FeedUsage() {}

    public FeedUsage(UUID flockId, UUID feedTypeId, LocalDate usageDate, Integer sacksUsed, String shift) {
        this.flockId = flockId;
        this.feedTypeId = feedTypeId;
        this.usageDate = usageDate;
        this.sacksUsed = sacksUsed;
        this.shift = shift;
    }

    // Getters & Setters
    public UUID getUsageId() { return usageId; }
    public void setUsageId(UUID usageId) { this.usageId = usageId; }

    public UUID getFlockId() { return flockId; }
    public void setFlockId(UUID flockId) { this.flockId = flockId; }

    public UUID getFeedTypeId() { return feedTypeId; }
    public void setFeedTypeId(UUID feedTypeId) { this.feedTypeId = feedTypeId; }

    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }

    public Integer getSacksUsed() { return sacksUsed; }
    public void setSacksUsed(Integer sacksUsed) { this.sacksUsed = sacksUsed; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}