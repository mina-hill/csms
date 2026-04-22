package com.csms.csms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "v_fcr_report")
@Immutable
public class VFcrReport {

    @Id
    @Column(name = "flock_id")
    private UUID flockId;

    @Column(name = "breed")
    private String breed;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "status")
    private String status;

    @Column(name = "total_sacks_used")
    private Integer totalSacksUsed;

    @Column(name = "total_feed_kg")
    private BigDecimal totalFeedKg;

    @Column(name = "weight_gain_kg")
    private BigDecimal weightGainKg;

    @Column(name = "fcr_value")
    private BigDecimal fcrValue;

    // Constructors
    public VFcrReport() {}

    // Getters only (immutable)
    public UUID getFlockId() { return flockId; }

    public String getBreed() { return breed; }

    public LocalDate getArrivalDate() { return arrivalDate; }

    public LocalDate getCloseDate() { return closeDate; }

    public String getStatus() { return status; }

    public Integer getTotalSacksUsed() { return totalSacksUsed; }

    public BigDecimal getTotalFeedKg() { return totalFeedKg; }

    public BigDecimal getWeightGainKg() { return weightGainKg; }

    public BigDecimal getFcrValue() { return fcrValue; }
}