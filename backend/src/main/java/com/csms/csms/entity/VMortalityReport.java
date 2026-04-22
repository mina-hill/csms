package com.csms.csms.entity;
import java.math.BigDecimal;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
@Entity
@Table(name = "v_mortality_report")
@Immutable
@IdClass(VMortalityReportId.class) // 1. Link the new ID class
public class VMortalityReport {

    @Id // 2. Keep this as ID
    @Column(name = "flock_id")
    private UUID flockId;

    @Id // 3. ADD THIS as an ID
    @Column(name = "record_date")
    private LocalDate recordDate;
    @Column(name = "breed")
    private String breed;

    @Column(name = "initial_qty")
    private Integer initialQty;

    
    @Column(name = "daily_deaths")
    private Integer dailyDeaths;

    @Column(name = "cumulative_deaths")
    private Integer cumulativeDeaths;

    @Column(name = "mortality_pct")
    private BigDecimal mortalityPct;

    public VMortalityReport() {}

    public UUID getFlockId() { return flockId; }
    public String getBreed() { return breed; }
    public Integer getInitialQty() { return initialQty; }
    public LocalDate getRecordDate() { return recordDate; }
    public Integer getDailyDeaths() { return dailyDeaths; }
    public Integer getCumulativeDeaths() { return cumulativeDeaths; }
    public BigDecimal getMortalityPct() { return mortalityPct; }
}