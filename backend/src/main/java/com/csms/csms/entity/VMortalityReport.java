package com.csms.csms.entity;
import java.math.BigDecimal;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
@Entity
@Table(name = "v_mortality_report")
@Immutable
public class VMortalityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "rowid")
    private Long rowId;

    @Column(name = "flock_id")
    private UUID flockId;

    @Column(name = "breed")
    private String breed;

    @Column(name = "initial_qty")
    private Integer initialQty;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "daily_deaths")
    private Integer dailyDeaths;

    @Column(name = "cumulative_deaths")
    private Integer cumulativeDeaths;

    @Column(name = "mortality_pct")
    private BigDecimal mortalityPct;

    // Constructors
    public VMortalityReport() {}

    // Getters only (immutable)
    public Long getRowId() { return rowId; }

    public UUID getFlockId() { return flockId; }

    public String getBreed() { return breed; }

    public Integer getInitialQty() { return initialQty; }

    public LocalDate getRecordDate() { return recordDate; }

    public Integer getDailyDeaths() { return dailyDeaths; }

    public Integer getCumulativeDeaths() { return cumulativeDeaths; }

    public BigDecimal getMortalityPct() { return mortalityPct; }
}