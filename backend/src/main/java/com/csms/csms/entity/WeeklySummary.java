package com.csms.csms.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "weekly_summary")
@DynamicInsert
public class WeeklySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "summary_id")
    private UUID summaryId;

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "chick_age_days", nullable = false)
    private Integer chickAgeDays;

    @Column(name = "total_chicks", nullable = false)
    private Integer totalChicks;

    @Column(name = "remaining_chicks", nullable = false)
    private Integer remainingChicks;

    @Column(name = "weekly_mortality", nullable = false)
    private Integer weeklyMortality = 0;

    @Column(name = "cumulative_mortality", nullable = false)
    private Integer cumulativeMortality = 0;

    // NUMERIC(6,3) — nullable (no weight yet)
    @Column(name = "avg_weight_kg", precision = 6, scale = 3)
    private BigDecimal avgWeightKg;

    @Column(name = "weekly_feed_sacks", nullable = false)
    private Integer weeklyFeedSacks = 0;

    @Column(name = "cumulative_feed_sacks", nullable = false)
    private Integer cumulativeFeedSacks = 0;

    // NUMERIC(8,3) — nullable (no weight available)
    @Column(name = "fcr_value", precision = 8, scale = 3)
    private BigDecimal fcrValue;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public WeeklySummary() {}

    public UUID getSummaryId()                           { return summaryId; }
    public void setSummaryId(UUID v)                     { this.summaryId = v; }

    public UUID getFlockId()                             { return flockId; }
    public void setFlockId(UUID v)                       { this.flockId = v; }

    public Integer getWeekNumber()                       { return weekNumber; }
    public void setWeekNumber(Integer v)                 { this.weekNumber = v; }

    public LocalDate getWeekEndDate()                    { return weekEndDate; }
    public void setWeekEndDate(LocalDate v)              { this.weekEndDate = v; }

    public Integer getChickAgeDays()                     { return chickAgeDays; }
    public void setChickAgeDays(Integer v)               { this.chickAgeDays = v; }

    public Integer getTotalChicks()                      { return totalChicks; }
    public void setTotalChicks(Integer v)                { this.totalChicks = v; }

    public Integer getRemainingChicks()                  { return remainingChicks; }
    public void setRemainingChicks(Integer v)            { this.remainingChicks = v; }

    public Integer getWeeklyMortality()                  { return weeklyMortality; }
    public void setWeeklyMortality(Integer v)            { this.weeklyMortality = v; }

    public Integer getCumulativeMortality()              { return cumulativeMortality; }
    public void setCumulativeMortality(Integer v)        { this.cumulativeMortality = v; }

    public BigDecimal getAvgWeightKg()                   { return avgWeightKg; }
    public void setAvgWeightKg(BigDecimal v)             { this.avgWeightKg = v; }

    public Integer getWeeklyFeedSacks()                  { return weeklyFeedSacks; }
    public void setWeeklyFeedSacks(Integer v)            { this.weeklyFeedSacks = v; }

    public Integer getCumulativeFeedSacks()              { return cumulativeFeedSacks; }
    public void setCumulativeFeedSacks(Integer v)        { this.cumulativeFeedSacks = v; }

    public BigDecimal getFcrValue()                      { return fcrValue; }
    public void setFcrValue(BigDecimal v)                { this.fcrValue = v; }

    public UUID getRecordedBy()                          { return recordedBy; }
    public void setRecordedBy(UUID v)                    { this.recordedBy = v; }

    public OffsetDateTime getCreatedAt()                 { return createdAt; }
}
